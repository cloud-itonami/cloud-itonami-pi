(ns pi.phase
  "Phase 0->3 staged rollout -- the payment-institution analog of
  `cloud-itonami-isic-6419`'s `banking.phase` / `cloud-itonami-isic-6910`'s
  `formation.phase`.

    Phase 0  read-only        -- no writes, still governor-gated.
    Phase 1  assisted-intake  -- account intake allowed, every write
                                 needs human approval.
    Phase 2  assisted-verify  -- adds AML/KYC verification, sanctions
                                 screening and PIS/AIS consent
                                 registration writes, still approval.
    Phase 3  supervised auto  -- governor-clean, high-confidence
                                 `:account/intake` (no capital risk yet)
                                 may auto-commit. `:actuation/execute-
                                 payment`/`:actuation/remit-payout` NEVER
                                 auto-commit, at any phase.

  `:actuation/execute-payment`/`:actuation/remit-payout` are deliberately
  ABSENT from every phase's `:auto` set, including phase 3 -- a permanent
  structural fact, not a rollout milestone still to come. Executing a
  real payment and posting a real remittance payout are the two
  real-world payment acts this actor performs; both are always a human
  operator call. `pi.governor`'s `:actuation/execute-payment`/
  `:actuation/remit-payout` high-stakes gate enforces the same invariant
  independently -- two layers, not one, agree on this. `:sanctions/
  screen` is likewise never auto-eligible, at any phase -- the same
  posture every sibling's screening op has."
  )

(def read-ops  #{})
(def write-ops #{:account/intake :compliance/verify :sanctions/screen
                 :consent/register-pis :consent/register-ais
                 :actuation/execute-payment :actuation/remit-payout})

;; NOTE the invariant: `:actuation/execute-payment`/`:actuation/remit-
;; payout` are members of `write-ops` (governor-gated like any write) but
;; are NEVER members of any phase's `:auto` set below. Do not add them
;; there.
(def phases
  "phase -> {:label .. :writes <ops allowed to write> :auto <ops allowed to
  auto-commit when governor-clean>}."
  {0 {:label "read-only"        :writes #{}                                                          :auto #{}}
   1 {:label "assisted-intake"  :writes #{:account/intake}                                           :auto #{}}
   2 {:label "assisted-verify"  :writes #{:account/intake :compliance/verify :sanctions/screen
                                          :consent/register-pis :consent/register-ais}                :auto #{}}
   3 {:label "supervised-auto"  :writes write-ops
      :auto #{:account/intake}}})

(def default-phase 3)

(defn gate
  "Adjust a governor disposition for the rollout phase. Returns
  {:disposition kw :reason kw|nil}.

  - reads pass through unchanged (phase restricts autonomy, not reads).
  - a governor HOLD always stays HOLD (compliance wins).
  - a write op not yet enabled in this phase -> HOLD (:phase-disabled).
  - a write op enabled but not auto-eligible -> ESCALATE (:phase-approval),
    even if the governor was clean.
  - `:actuation/execute-payment`/`:actuation/remit-payout` are never
    auto-eligible at any phase, so they always escalate once the governor
    clears them (or hold if the governor doesn't)."
  [phase {:keys [op]} governor-disposition]
  (let [{:keys [writes auto]} (get phases phase (get phases default-phase))]
    (cond
      (= :hold governor-disposition)       {:disposition :hold :reason nil}
      (contains? read-ops op)              {:disposition governor-disposition :reason nil}
      (not (contains? writes op))          {:disposition :hold :reason :phase-disabled}
      (and (= :commit governor-disposition)
           (not (contains? auto op)))      {:disposition :escalate :reason :phase-approval}
      :else                                {:disposition governor-disposition :reason nil})))

(defn verdict->disposition
  "Map a PIGovernor verdict to a base disposition before the phase gate."
  [verdict]
  (cond (:hard? verdict) :hold
        (:escalate? verdict) :escalate
        :else :commit))
