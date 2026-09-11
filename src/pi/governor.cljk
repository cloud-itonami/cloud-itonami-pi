(ns pi.governor
  "PIGovernor -- the independent compliance layer that earns the
  PaymentOps-LLM the right to commit. The LLM has no notion of which
  jurisdiction's PSD2 (or equivalent) payment-services regime is official,
  whether a payment account's own IBAN actually passes its own ISO 7064
  MOD 97-10 checksum, whether a sanctions flag against an account has
  actually stayed unresolved, whether a required PIS consent actually
  exists on file, or when an act stops being a draft and becomes a real
  payment execution or a real remittance payout, so this MUST be a
  separate system able to *reject* a proposal and fall back to HOLD -- the
  payment-institution analog of `cloud-itonami-isic-6419`'s Monetary
  Intermediation Governor.

  Eight checks, in priority order. The first six are HARD violations: a
  human approver CANNOT override them (you don't get to approve your way
  past a fabricated spec-basis, an invalid IBAN checksum, an unresolved
  sanctions flag, a missing PIS consent, or a double execution/payout).
  The last two are SOFT: they ask a human to look (low confidence /
  actuation), and the human may approve -- but see `pi.phase`: for
  `:stake :actuation/execute-payment`/`:actuation/remit-payout` (a real
  payment execution or a real remittance payout) NO phase ever allows
  auto-commit either. Two independent layers agree that actuation is
  always a human call.

    1. Effect matches op   -- does the proposal's :effect (what actually
                               gets written to the SSoT on commit) match
                               the ONE legitimate effect for the
                               REQUEST's :op (`op->effect`)? Every check
                               below keys off the request's :op, not the
                               proposal's self-reported :effect --
                               without this check first, an advisor (an
                               untrusted, possibly hallucinating real LLM)
                               could answer a harmless-looking
                               :compliance/verify request with `:effect
                               :account/mark-executed`, and a human
                               approving what looks like a compliance
                               check would silently trigger a REAL
                               payment execution with none of
                               :actuation/execute-payment's own scrutiny
                               ever run (the same class of bug
                               `cloud-itonami-isic-6910`'s ADR-0001
                               Addendum 12 found and fixed the hard way;
                               this actor is built with the fix from day
                               one instead of re-discovering it).
    2. Spec-basis           -- did the compliance/consent/actuation
                               proposal cite an OFFICIAL source
                               (`pi.facts`), or invent one?
    3. Evidence incomplete   -- for `:actuation/execute-payment`/
                               `:actuation/remit-payout`, has the account
                               actually been assessed with a full
                               identity-verification/source-of-funds/
                               safeguarding-of-funds-arrangement/
                               sanctions-screening evidence checklist on
                               file?
    4. IBAN checksum invalid -- for `:actuation/execute-payment`,
                               INDEPENDENTLY recompute whether the
                               account's own IBAN passes ISO 7064 MOD
                               97-10 (`pi.registry/iban-checksum-
                               invalid?`); for `:actuation/remit-payout`,
                               the same recompute against the
                               beneficiary's IBAN -- needs no proposal
                               inspection at all.
    5. Sanctions flag unresolved -- reported by THIS proposal itself (a
                               `:sanctions/screen` that just found one),
                               or already on file for the account
                               (`:sanctions/screen`/
                               `:actuation/execute-payment`/
                               `:actuation/remit-payout`). Evaluated
                               UNCONDITIONALLY (not scoped to a specific
                               op) so the screening op itself can
                               HARD-hold on its own finding -- REUSES the
                               exact `sanctions-violations` concept/name
                               `banking.governor`/`underwriting.
                               governor`/`casualty.governor`/`vcfund.
                               governor`/`formation.governor`/`realty.
                               governor` already established -- sanctions
                               screening is a genuinely SHARED,
                               industry-standard AML/OFAC compliance
                               concept across financial-services
                               verticals.
    6. PIS consent missing  -- for `:actuation/execute-payment` where
                               the REQUEST declares `:channel :pis`
                               (a third-party payment-initiation-service
                               flow against an account the customer holds
                               at ANOTHER ASPSP via PSD2 XS2A access,
                               as distinct from the PI executing a
                               payment directly on its own customer's
                               payment account), a valid, registered PIS
                               consent must actually be on file
                               (`pi.store/consent-of` `:pis`, `:status
                               :active`) -- PSD2's whole legal basis for
                               a PIS-initiated payment IS the customer's
                               explicit consent; a PIS-channel execution
                               with no consent on file is not a lesser
                               violation of some other rule, it has NO
                               legal basis at all.
    7. Confidence floor / actuation
       gate                  -- LLM confidence below threshold, OR the op
                               is `:actuation/execute-payment`/
                               `:actuation/remit-payout` (REAL payment
                               acts) -> escalate.

  Two more guards, double-execution/double-payout prevention, are
  enforced but NOT listed as numbered HARD checks above because they need
  no upstream comparison at all -- `already-executed-violations`/
  `already-remitted-violations` refuse to execute a payment/post a payout
  for the SAME account twice, off dedicated `:payment-executed?`/
  `:remittance-payout-posted?` facts (never a `:status` value) -- the SAME
  'check a dedicated boolean, not status' discipline every prior sibling
  governor's guards establish, informed by `cloud-itonami-isic-6492`'s
  status-lifecycle bug (ADR-2607071320)."
  (:require [pi.facts :as facts]
            [pi.registry :as registry]
            [pi.store :as store]))

(def confidence-floor 0.6)

(def high-stakes
  "Stakes grave enough to always require a human, even when clean.
  Executing a real payment and posting a real remittance payout are the
  two real-world actuation events this actor performs -- a two-member
  set, matching every prior dual-actuation sibling's shape. Both are
  POSITIVE actuations (executing/posting a real record), matching this
  fleet's majority actuation shape."
  #{:actuation/execute-payment :actuation/remit-payout})

;; ----------------------------- checks -----------------------------

(def op->effect
  "The ONE legitimate `:effect` a proposal may declare for each op --
  `pi.operation/commit-record` takes `:effect` straight from the
  (untrusted) advisor proposal with no cross-check of its own, so this
  table is the only thing standing between 'the request says
  :compliance/verify' and 'the SSoT mutation that actually runs is
  :account/mark-executed'."
  {:account/intake         :account/upsert
   :compliance/verify      :compliance/set
   :sanctions/screen       :sanctions-screen/set
   :consent/register-pis   :consent/mark-registered-pis
   :consent/register-ais   :consent/mark-registered-ais
   :actuation/execute-payment :account/mark-executed
   :actuation/remit-payout    :account/mark-remitted})

(defn- effect-mismatch-violations
  "HARD, checked first: a proposal whose :effect is not the one paired
  with the request's :op in `op->effect` is rejected outright, before any
  op-specific check below even runs -- see `op->effect`'s docstring."
  [{:keys [op]} proposal]
  (when-let [expected (op->effect op)]
    (when (not= expected (:effect proposal))
      [{:rule :effect-mismatch
        :detail (str "op " op " の提案は :effect " expected
                     " のはずが実際には " (:effect proposal) " になっている")}])))

(defn- spec-basis-violations
  "A `:compliance/verify`, `:consent/register-pis`, `:consent/register-
  ais`, `:actuation/execute-payment` or `:actuation/remit-payout`
  proposal with no spec-basis citation is a HARD violation -- never
  invent a jurisdiction's payment-services licensing/AML requirements."
  [{:keys [op]} proposal]
  (when (contains? #{:compliance/verify :consent/register-pis :consent/register-ais
                     :actuation/execute-payment :actuation/remit-payout} op)
    (let [value (:value proposal)]
      (when (or (empty? (:cites proposal))
                (and (contains? value :spec-basis) (nil? (:spec-basis value))))
        [{:rule :no-spec-basis
          :detail "公式spec-basisの引用が無い提案はPSD2/決済サービス業運営基準として扱えない"}]))))

(defn- evidence-incomplete-violations
  "For `:actuation/execute-payment`/`:actuation/remit-payout`, the
  jurisdiction's required identity-verification/source-of-funds/
  safeguarding-of-funds-arrangement/sanctions-screening evidence must
  actually be satisfied -- do not trust the advisor's self-reported
  confidence alone."
  [{:keys [op subject]} st]
  (when (contains? #{:actuation/execute-payment :actuation/remit-payout} op)
    (let [a (store/account st subject)
          compliance (store/compliance-of st subject)]
      (when-not (and compliance
                     (facts/required-evidence-satisfied?
                      (:jurisdiction a) (:checklist compliance)))
        [{:rule :evidence-incomplete
          :detail "法域の必要書類(本人確認記録/資金源確認記録/資金保全記録/制裁リストスクリーニング記録等)が充足していない状態での提案"}]))))

(defn- iban-checksum-invalid-violations
  "For `:actuation/execute-payment`, INDEPENDENTLY recompute whether the
  account's own IBAN passes ISO 7064 MOD 97-10; for `:actuation/remit-
  payout`, the same recompute against the beneficiary's IBAN
  (`pi.registry/iban-checksum-invalid?`) -- needs no proposal inspection
  at all, since its input is a permanent ground-truth field already on
  the account."
  [{:keys [op subject]} st]
  (let [a (store/account st subject)]
    (cond
      (= op :actuation/execute-payment)
      (when (registry/iban-checksum-invalid? a)
        [{:rule :iban-checksum-invalid
          :detail (str subject " のIBAN(" (:iban a) ")がISO 7064 MOD 97-10検査に不合格")}])

      (= op :actuation/remit-payout)
      (when (registry/iban-checksum-invalid? {:iban (:beneficiary-iban a)})
        [{:rule :iban-checksum-invalid
          :detail (str subject " の受取人IBAN(" (:beneficiary-iban a) ")がISO 7064 MOD 97-10検査に不合格")}]))))

(defn- sanctions-violations
  "An unresolved sanctions flag -- reported by THIS proposal (e.g. a
  `:sanctions/screen` that itself just found one), or already on file in
  the store for the account (`:sanctions/screen`/`:actuation/execute-
  payment`/`:actuation/remit-payout`) -- is a HARD, un-overridable hold.
  Evaluated UNCONDITIONALLY (not scoped to a specific op) so the
  screening op itself can HARD-hold on its own finding."
  [{:keys [op subject]} proposal st]
  (let [hit-in-proposal? (= :unresolved (get-in proposal [:value :verdict]))
        account-id (when (contains? #{:sanctions/screen :actuation/execute-payment
                                      :actuation/remit-payout} op)
                     subject)
        hit-on-file? (and account-id (= :unresolved (:verdict (store/sanctions-screen-of st account-id))))]
    (when (or hit-in-proposal? hit-on-file?)
      [{:rule :sanctions-flag-unresolved
        :detail "未解決の制裁リストフラグがある口座に対する決済/送金提案は進められない"}])))

(defn- pis-consent-missing-violations
  "For `:actuation/execute-payment` where the REQUEST declares `:channel
  :pis` (third-party payment-initiation-service flow against an account
  held at another ASPSP), a valid, registered PIS consent must actually
  be on file -- PSD2's legal basis for a PIS-initiated payment IS the
  customer's explicit consent. A direct-channel execution (the PI's own
  customer, own payment account) needs no such consent -- it is not
  reading/initiating against a third-party ASPSP."
  [{:keys [op subject channel]} st]
  (when (and (= op :actuation/execute-payment) (= channel :pis))
    (let [c (store/consent-of st subject :pis)]
      (when-not (= :active (:status c))
        [{:rule :pis-consent-missing
          :detail (str subject " のPIS(決済指図伝達サービス)同意が登録されていない状態でのPISチャネル決済実行提案")}]))))

(defn- already-executed-violations
  "For `:actuation/execute-payment`, refuses to execute a payment for the
  SAME account twice, off a dedicated `:payment-executed?` fact (never a
  `:status` value)."
  [{:keys [op subject]} st]
  (when (= op :actuation/execute-payment)
    (when (store/account-already-executed? st subject)
      [{:rule :already-executed
        :detail (str subject " は既に決済実行済み")}])))

(defn- already-remitted-violations
  "For `:actuation/remit-payout`, refuses to post a remittance payout for
  the SAME account twice, off a dedicated `:remittance-payout-posted?`
  fact (never a `:status` value)."
  [{:keys [op subject]} st]
  (when (= op :actuation/remit-payout)
    (when (store/account-already-remitted? st subject)
      [{:rule :already-remitted
        :detail (str subject " は既に送金実行済み")}])))

(defn check
  "Censors a PaymentOps-LLM proposal against the governor rules. Returns
  {:ok? bool :violations [..] :confidence c :escalate? bool :high-stakes? bool
   :hard? bool}.

   - :hard?       -- at least one HARD violation. Forces HOLD; a human
                    cannot override.
   - :escalate?   -- soft: low confidence OR actuation. A human decides.
   - :ok?         -- clean AND not escalating: safe to auto-commit."
  [request _context proposal st]
  (let [hard (into []
                   (concat (effect-mismatch-violations request proposal)
                           (spec-basis-violations request proposal)
                           (evidence-incomplete-violations request st)
                           (iban-checksum-invalid-violations request st)
                           (sanctions-violations request proposal st)
                           (pis-consent-missing-violations request st)
                           (already-executed-violations request st)
                           (already-remitted-violations request st)))
        conf (:confidence proposal 0.0)
        low? (< conf confidence-floor)
        stakes? (boolean (high-stakes (:stake proposal)))
        hard? (boolean (seq hard))]
    {:ok?          (and (not hard?) (not low?) (not stakes?))
     :violations   hard
     :confidence   conf
     :hard?        hard?
     :escalate?    (and (not hard?) (or low? stakes?))
     :high-stakes? stakes?}))

(defn hold-fact
  "The audit fact written when a proposal is rejected (HOLD)."
  [request context verdict]
  {:t          :governor-hold
   :op         (:op request)
   :actor      (:actor-id context)
   :subject    (:subject request)
   :disposition :hold
   :basis      (mapv :rule (:violations verdict))
   :violations (:violations verdict)
   :confidence (:confidence verdict)})
