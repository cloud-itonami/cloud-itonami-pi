(ns pi.registry
  "Pure-function draft-record construction for the two real-world
  actuation events this actor performs -- payment execution (PSD2 Annex I
  services 2/3/6: execution of payment transactions, including payment
  initiation) and remittance payout (PSD2 Annex I service 5: money
  remittance) -- plus PIS/AIS consent-record construction.

  Like `cloud-itonami-isic-6419` (banking), this domain HAS a single
  international check-digit standard for a payment account's primary
  identifier -- IBAN (ISO 13616), whose own check digits are ISO 7064
  MOD 97-10. `iban-checksum-invalid?` re-implements this REAL algorithm
  (not a fabricated placeholder) independently in THIS repo (a standalone,
  forkable actor should not need a compile-time dependency on a sibling
  actor's internals just to recompute a public ISO standard) -- the same
  discipline `cloud-itonami-isic-6419`'s `banking.registry` established as
  the fleet's first checksum/format-validity check family member. This
  actor is the SECOND independent implementation of that same real
  algorithm, applied to a different (but IBAN-identified) domain object: a
  PI's payment account and a remittance beneficiary account, rather than a
  bank's own settlement account.

  Every OTHER reference number this actor issues (payment-execution /
  remittance-payout record IDs, PIS/AIS consent IDs) has no such single
  international check-digit standard -- every payment scheme/jurisdiction
  assigns its own reference format, the same honest, non-fabricating
  discipline `pi.facts` uses; this namespace does NOT invent one for
  those, it builds a jurisdiction-scoped sequence number instead.

  This namespace is pure data + pure functions -- no I/O, no network call
  to any real payment rail, ASPSP, or open-banking gateway. It builds the
  RECORD a Payment Institution operator would keep, not the act of
  executing the payment or dispatching the remittance payout itself (that
  is `pi.operation`'s `:actuation/execute-payment`/`:actuation/remit-
  payout`, always human-gated -- see README `Actuation`)."
  (:require [kotoba.lang.text :as str]))

(defn- unsigned-certificate
  "Every certificate this actor produces is UNSIGNED -- signature is the
  Payment Institution operator's own act, not this actor's. See README
  `Actuation`."
  [kind subject record-id]
  {"@context" ["https://www.w3.org/ns/credentials/v2"]
   "type" ["VerifiableCredential" kind]
   "credentialSubject" {"id" subject "record" record-id}
   "proof" nil
   "issued_by_registry" false
   "status" "draft-unsigned"})

(defn- zero-pad [n w]
  (let [s (str n)]
    (str (apply str (repeat (max 0 (- w (count s))) "0")) s)))

(def ^:private digit-chars (set "0123456789"))
(def ^:private alphabet "ABCDEFGHIJKLMNOPQRSTUVWXYZ")
(def ^:private digit-value
  {"0" 0 "1" 1 "2" 2 "3" 3 "4" 4 "5" 5 "6" 6 "7" 7 "8" 8 "9" 9})

(defn- char->digits
  "ISO 7064 MOD 97-10 letter substitution: A=10 .. Z=35, digits pass
  through unchanged, as decimal-string fragments. Implemented via portable
  `clojure.string` lookups (no JVM-only `Character` interop) so this
  namespace stays a real `.cljc` -- runs on JVM/SCI/ClojureScript alike."
  [c]
  (if (contains? digit-chars c)
    (str c)
    (str (+ 10 (str/index-of alphabet (str c))))))

(defn- iban-numeric-string
  "Move the first 4 characters to the end, then substitute letters for
  digits per ISO 7064 MOD 97-10 -- the standard IBAN validation
  rearrangement."
  [iban]
  (let [cleaned (str/replace (str/upper iban) #"\s" "")
        rearranged (str (subs cleaned 4) (subs cleaned 0 4))]
    (apply str (map char->digits rearranged))))

(defn- mod-97
  "Remainder of `numeric-string` (a decimal string, possibly far larger
  than any native integer) modulo 97, computed digit-by-digit so no
  bignum library is required -- the standard streaming-mod-97 technique."
  [numeric-string]
  (reduce (fn [acc i]
            (mod (+ (* acc 10) (digit-value (subs numeric-string i (inc i)))) 97))
          0
          (range (count numeric-string))))

(defn iban-checksum-invalid?
  "Does `account`'s own `:iban` fail ISO 7064 MOD 97-10 validation? A
  valid IBAN's rearranged numeric form is congruent to 1 mod 97 -- any
  other remainder (or a non-conforming shape: not 15-34 chars, doesn't
  start with 2 letters + 2 digits) means the IBAN is invalid. A pure
  ground-truth check against the account's own `:iban` field -- no
  upstream comparison or second field needed."
  [{:keys [iban]}]
  (or (nil? iban)
      (not (re-matches #"[A-Za-z]{2}\d{2}[A-Za-z0-9]{11,30}" (str/replace (str iban) #"\s" "")))
      (not= 1 (mod-97 (iban-numeric-string iban)))))

(defn register-payment-execution
  "Validate + construct the PAYMENT-EXECUTION registration DRAFT --
  covering PSD2 Annex I services 2/3/6 (execution of payment transactions,
  including where covered by a credit line, and payment initiation
  services). Pure function -- does not touch any real payment rail or
  ASPSP; it builds the RECORD a PI operator would keep. `pi.governor`
  independently re-verifies the account's own IBAN checksum, sanctions
  resolution, and blocks a double-execution for the same payment, before
  this is ever allowed to commit."
  [account-id jurisdiction sequence]
  (when-not (and account-id (not= account-id ""))
    (throw (ex-info "payment-execution: account_id required" {})))
  (when-not (and jurisdiction (not= jurisdiction ""))
    (throw (ex-info "payment-execution: jurisdiction required" {})))
  (when (< sequence 0)
    (throw (ex-info "payment-execution: sequence must be >= 0" {})))
  (let [execution-number (str (str/upper jurisdiction) "-PMT-" (zero-pad sequence 6))
        record {"record_id" execution-number
                "kind" "payment-execution-draft"
                "account_id" account-id
                "jurisdiction" jurisdiction
                "immutable" true}]
    {"record" record "execution_number" execution-number
     "certificate" (unsigned-certificate "PaymentExecution" execution-number execution-number)}))

(defn register-remittance-payout
  "Validate + construct the REMITTANCE-PAYOUT registration DRAFT --
  covering PSD2 Annex I service 5 (money remittance): disbursement to a
  beneficiary who is not necessarily a PI customer with a full payment
  account relationship. Pure function -- does not touch any real payment
  rail; it builds the RECORD a PI operator would keep. `pi.governor`
  independently re-verifies the beneficiary's own IBAN checksum,
  sanctions resolution, and blocks a double-payout for the same
  remittance, before this is ever allowed to commit."
  [account-id jurisdiction sequence]
  (when-not (and account-id (not= account-id ""))
    (throw (ex-info "remittance-payout: account_id required" {})))
  (when-not (and jurisdiction (not= jurisdiction ""))
    (throw (ex-info "remittance-payout: jurisdiction required" {})))
  (when (< sequence 0)
    (throw (ex-info "remittance-payout: sequence must be >= 0" {})))
  (let [payout-number (str (str/upper jurisdiction) "-REM-" (zero-pad sequence 6))
        record {"record_id" payout-number
                "kind" "remittance-payout-draft"
                "account_id" account-id
                "jurisdiction" jurisdiction
                "immutable" true}]
    {"record" record "payout_number" payout-number
     "certificate" (unsigned-certificate "RemittancePayout" payout-number payout-number)}))

(defn register-consent
  "Validate + construct a PIS/AIS CONSENT registration DRAFT -- covering
  PSD2 Annex I services 6/7 (payment initiation services, account
  information services). `kind` is `:pis` or `:ais`. A consent is NOT an
  actuation record (no funds move; no account-information read happens
  here) -- it is the customer's explicit, revocable authorization that a
  LATER `:actuation/execute-payment` (for :pis) or account-information
  read (for :ais) may rely on. Pure function -- does not touch any real
  open-banking (XS2A) gateway."
  [account-id kind jurisdiction sequence]
  (when-not (and account-id (not= account-id ""))
    (throw (ex-info "consent: account_id required" {})))
  (when-not (contains? #{:pis :ais} kind)
    (throw (ex-info "consent: kind must be :pis or :ais" {})))
  (when-not (and jurisdiction (not= jurisdiction ""))
    (throw (ex-info "consent: jurisdiction required" {})))
  (when (< sequence 0)
    (throw (ex-info "consent: sequence must be >= 0" {})))
  (let [tag (if (= kind :pis) "PIS" "AIS")
        consent-number (str (str/upper jurisdiction) "-" tag "-" (zero-pad sequence 6))
        record {"record_id" consent-number
                "kind" (str (str/lower tag) "-consent-draft")
                "account_id" account-id
                "jurisdiction" jurisdiction
                "immutable" true}]
    {"record" record "consent_number" consent-number
     "certificate" (unsigned-certificate (str (if (= kind :pis) "Pis" "Ais") "Consent")
                                         consent-number consent-number)}))

(defn append [history result]
  (conj (vec history) (get result "record")))
