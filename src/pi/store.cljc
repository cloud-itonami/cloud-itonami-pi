(ns pi.store
  "SSoT for the PI actor, behind a `Store` protocol so the backend is a
  swap, not a rewrite -- the same seam every prior `cloud-itonami-*` actor
  in this fleet uses. This actor is MemStore-only at this maturity stage
  (an atom of EDN, the deterministic default for dev/tests/demo, no deps)
  -- a `DatomicStore` (`langchain.db`-backed, swappable to a real Datomic
  Local or a kotoba-server pod) is the natural next seam, following the
  exact `:db-api`-driven pattern `cloud-itonami-isic-6419`/`-6910` already
  proved out, but is deliberately deferred rather than spoken for here
  (see README `Maturity`).

  Like `cloud-itonami-isic-6419`, this actor has TWO actuation events
  (executing a payment, posting a remittance payout) acting on the SAME
  entity (a payment account), each with its OWN history collection,
  sequence counter and dedicated double-actuation-guard boolean
  (`:payment-executed?`/`:remittance-payout-posted?`, never a `:status`
  value) -- the same discipline every prior sibling governor's guards
  establish, informed by `cloud-itonami-isic-6492`'s status-lifecycle bug
  (ADR-2607071320). PIS/AIS consent registration is NOT an actuation (no
  funds move, no external account is read here) but it DOES produce its
  own formatted, sequenced draft record via `pi.registry/register-
  consent` -- the same 'draft record built at commit time, not proposal
  time' discipline the two actuation ops use, because a consent record
  needs a stable, jurisdiction-scoped consent-number just like a
  settlement or interbank-message record does.

  The ledger stays append-only: 'which account was screened for an
  unresolved sanctions flag, which PIS/AIS consent was registered, which
  payment was executed, which remittance was paid out, on what
  jurisdictional basis, approved by whom' is always a query over an
  immutable log -- the audit trail a customer trusting a Payment
  Institution with their money needs, and the evidence an operator needs
  if an execution or payout decision is later disputed."
  (:require [pi.registry :as registry]))

(defprotocol Store
  (account [s id])
  (all-accounts [s])
  (sanctions-screen-of [s account-id] "committed sanctions-screening verdict for an account, or nil")
  (compliance-of [s account-id] "committed AML/KYC evidence assessment, or nil")
  (consent-of [s account-id kind] "committed PIS/AIS consent record (kind :pis|:ais) for an account, or nil")
  (ledger [s])
  (payment-execution-history [s] "the append-only payment-execution history (pi.registry drafts)")
  (remittance-payout-history [s] "the append-only remittance-payout history (pi.registry drafts)")
  (consent-history [s] "the append-only PIS/AIS consent history (pi.registry drafts)")
  (next-payment-sequence [s jurisdiction] "next payment-execution-number sequence for a jurisdiction")
  (next-remittance-sequence [s jurisdiction] "next remittance-payout-number sequence for a jurisdiction")
  (next-consent-sequence [s jurisdiction kind] "next consent-number sequence for a jurisdiction+kind")
  (account-already-executed? [s account-id] "has this account's payment already been executed?")
  (account-already-remitted? [s account-id] "has this account's remittance payout already been posted?")
  (commit-record! [s record] "apply a committed op's record to the SSoT")
  (append-ledger! [s fact]   "append one immutable decision fact")
  (with-accounts [s accounts] "replace/seed the account directory (map id->account)"))

;; ----------------------------- demo data -----------------------------

(defn demo-data
  "A small, self-contained account set covering both actuation lifecycles
  (executing a payment, posting a remittance payout) so the actor + tests
  run offline. `:iban`/`:beneficiary-iban` values include one real, valid
  published IBAN test vector (Deutsche Bundesbank's own
  DE89370400440532013000) and one deliberately corrupted variant."
  []
  {:accounts
   {"account-1" {:id "account-1" :holder-name "Sato Ichiro"
                :iban "DE89370400440532013000"
                :beneficiary-iban "DE89370400440532013000"
                :sanctions-flag-unresolved? false
                :payment-executed? false :remittance-payout-posted? false
                :jurisdiction "JPN" :status :intake}
    "account-2" {:id "account-2" :holder-name "Atlantis Doe"
                :iban "DE89370400440532013000"
                :beneficiary-iban "DE89370400440532013000"
                :sanctions-flag-unresolved? false
                :payment-executed? false :remittance-payout-posted? false
                :jurisdiction "ATL" :status :intake}
    "account-3" {:id "account-3" :holder-name "鈴木健太"
                :iban "DE89370400440532013099"
                :beneficiary-iban "DE89370400440532013099"
                :sanctions-flag-unresolved? false
                :payment-executed? false :remittance-payout-posted? false
                :jurisdiction "GBR" :status :intake}
    "account-4" {:id "account-4" :holder-name "田中麻衣"
                :iban "DE89370400440532013000"
                :beneficiary-iban "DE89370400440532013000"
                :sanctions-flag-unresolved? true
                :payment-executed? false :remittance-payout-posted? false
                :jurisdiction "JPN" :status :intake}}})

;; ----------------------------- shared commit logic -----------------------------

(defn- execute-payment!
  "Backend-agnostic `:account/mark-executed` -- looks up the account via
  the protocol and drafts the payment-execution record, and returns
  {:result .. :account-patch ..} for the caller to persist."
  [s account-id]
  (let [a (account s account-id)
        seq-n (next-payment-sequence s (:jurisdiction a))
        result (registry/register-payment-execution account-id (:jurisdiction a) seq-n)]
    {:result result
     :account-patch {:payment-executed? true
                    :execution-number (get result "execution_number")}}))

(defn- remit-payout!
  "Backend-agnostic `:account/mark-remitted` -- looks up the account via
  the protocol and drafts the remittance-payout record, and returns
  {:result .. :account-patch ..} for the caller to persist."
  [s account-id]
  (let [a (account s account-id)
        seq-n (next-remittance-sequence s (:jurisdiction a))
        result (registry/register-remittance-payout account-id (:jurisdiction a) seq-n)]
    {:result result
     :account-patch {:remittance-payout-posted? true
                    :payout-number (get result "payout_number")}}))

(defn- register-consent!
  "Backend-agnostic `:consent/mark-registered` -- looks up the account via
  the protocol and drafts the PIS/AIS consent record, and returns
  {:result .. :consent-status ..} for the caller to persist. `kind` is
  `:pis` or `:ais`."
  [s account-id kind]
  (let [a (account s account-id)
        seq-n (next-consent-sequence s (:jurisdiction a) kind)
        result (registry/register-consent account-id kind (:jurisdiction a) seq-n)]
    {:result result
     :consent-status {:account-id account-id :kind kind :status :active
                      :consent-number (get result "consent_number")
                      :jurisdiction (:jurisdiction a)}}))

;; ----------------------------- MemStore (default) -----------------------------

(defrecord MemStore [a]
  Store
  (account [_ id] (get-in @a [:accounts id]))
  (all-accounts [_] (sort-by :id (vals (:accounts @a))))
  (sanctions-screen-of [_ id] (get-in @a [:sanctions-screens id]))
  (compliance-of [_ account-id] (get-in @a [:compliance-assessments account-id]))
  (consent-of [_ account-id kind] (get-in @a [:consents account-id kind]))
  (ledger [_] (:ledger @a))
  (payment-execution-history [_] (:payment-executions @a))
  (remittance-payout-history [_] (:remittance-payouts @a))
  (consent-history [_] (:consents-history @a))
  (next-payment-sequence [_ jurisdiction] (get-in @a [:payment-sequences jurisdiction] 0))
  (next-remittance-sequence [_ jurisdiction] (get-in @a [:remittance-sequences jurisdiction] 0))
  (next-consent-sequence [_ jurisdiction kind] (get-in @a [:consent-sequences [jurisdiction kind]] 0))
  (account-already-executed? [_ account-id] (boolean (get-in @a [:accounts account-id :payment-executed?])))
  (account-already-remitted? [_ account-id] (boolean (get-in @a [:accounts account-id :remittance-payout-posted?])))
  (commit-record! [s {:keys [effect path value payload]}]
    (case effect
      :account/upsert
      (swap! a update-in [:accounts (:id value)] merge value)

      :compliance/set
      (swap! a assoc-in [:compliance-assessments (first path)] payload)

      :sanctions-screen/set
      (swap! a assoc-in [:sanctions-screens (first path)] payload)

      :consent/mark-registered-pis
      (let [account-id (first path)
            {:keys [result consent-status]} (register-consent! s account-id :pis)
            jurisdiction (:jurisdiction consent-status)]
        (swap! a (fn [state]
                   (-> state
                       (update-in [:consent-sequences [jurisdiction :pis]] (fnil inc 0))
                       (assoc-in [:consents account-id :pis] consent-status)
                       (update :consents-history registry/append result))))
        result)

      :consent/mark-registered-ais
      (let [account-id (first path)
            {:keys [result consent-status]} (register-consent! s account-id :ais)
            jurisdiction (:jurisdiction consent-status)]
        (swap! a (fn [state]
                   (-> state
                       (update-in [:consent-sequences [jurisdiction :ais]] (fnil inc 0))
                       (assoc-in [:consents account-id :ais] consent-status)
                       (update :consents-history registry/append result))))
        result)

      :account/mark-executed
      (let [account-id (first path)
            {:keys [result account-patch]} (execute-payment! s account-id)
            jurisdiction (:jurisdiction (account s account-id))]
        (swap! a (fn [state]
                   (-> state
                       (update-in [:payment-sequences jurisdiction] (fnil inc 0))
                       (update-in [:accounts account-id] merge account-patch)
                       (update :payment-executions registry/append result))))
        result)

      :account/mark-remitted
      (let [account-id (first path)
            {:keys [result account-patch]} (remit-payout! s account-id)
            jurisdiction (:jurisdiction (account s account-id))]
        (swap! a (fn [state]
                   (-> state
                       (update-in [:remittance-sequences jurisdiction] (fnil inc 0))
                       (update-in [:accounts account-id] merge account-patch)
                       (update :remittance-payouts registry/append result))))
        result)
      nil)
    s)
  (append-ledger! [_ fact] (swap! a update :ledger conj fact) fact)
  (with-accounts [s accounts] (when (seq accounts) (swap! a assoc :accounts accounts)) s))

(defn seed-db
  "A MemStore seeded with the demo account set. The deterministic default."
  []
  (->MemStore (atom (assoc (demo-data)
                           :compliance-assessments {} :sanctions-screens {} :consents {}
                           :consents-history [] :ledger [] :payment-sequences {} :payment-executions []
                           :remittance-sequences {} :remittance-payouts []
                           :consent-sequences {}))))
