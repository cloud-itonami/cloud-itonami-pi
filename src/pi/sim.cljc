(ns pi.sim
  "Demo driver -- `clojure -M:dev:run`. Walks a clean account through
  intake -> AML/KYC compliance verification -> sanctions screening ->
  PIS consent registration -> a PIS-channel payment-execution proposal
  (always escalates) -> human approval -> commit -> a money-remittance
  payout (also always escalates), then shows two HARD holds (an
  unresolved sanctions flag, a fabricated jurisdiction) that never reach
  a human at all, and prints the audit ledger + the draft
  payment-execution/remittance-payout record history."
  (:require [langgraph.graph :as g]
            [pi.store :as store]
            [pi.operation :as op]))

(def operator {:actor-id "op-1" :actor-role :pi-operator :phase 3})

(defn- exec! [actor tid request context]
  (g/run* actor {:request request :context context} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}} {:thread-id tid :resume? true}))

(defn -main [& _]
  (let [db (store/seed-db)
        actor (op/build db)]
    (println "== intake account-1 (JPN, clean holder) ==")
    (println (exec! actor "t1" {:op :account/intake :subject "account-1"
                                :patch {:id "account-1" :status :ready}} operator))

    (println "== compliance/verify account-1 (escalates -- human approves) ==")
    (println (exec! actor "t2" {:op :compliance/verify :subject "account-1"} operator))
    (println (approve! actor "t2"))

    (println "== sanctions/screen account-1 (clean; escalates -- human approves) ==")
    (println (exec! actor "t3" {:op :sanctions/screen :subject "account-1"} operator))
    (println (approve! actor "t3"))

    (println "== consent/register-pis account-1 (escalates -- human approves) ==")
    (println (exec! actor "t3b" {:op :consent/register-pis :subject "account-1"} operator))
    (println (approve! actor "t3b"))

    (println "== actuation/execute-payment account-1, PIS channel (always escalates -- actuation) ==")
    (let [r (exec! actor "t4" {:op :actuation/execute-payment :subject "account-1" :channel :pis} operator)]
      (println r)
      (println "-- human operator approves --")
      (println (approve! actor "t4")))

    (println "== actuation/remit-payout account-1 (always escalates -- actuation) ==")
    (let [r (exec! actor "t5" {:op :actuation/remit-payout :subject "account-1"} operator)]
      (println r)
      (println "-- human operator approves --")
      (println (approve! actor "t5")))

    (println "== actuation/execute-payment account-1 AGAIN (already executed -> HARD hold, never reaches a human) ==")
    (println (exec! actor "t6" {:op :actuation/execute-payment :subject "account-1"} operator))

    (println "== sanctions/screen account-4 (unresolved flag -> HARD hold, never reaches a human) ==")
    (println (exec! actor "t7" {:op :sanctions/screen :subject "account-4"} operator))

    (println "== compliance/verify account-2 (no spec-basis -> HARD hold) ==")
    (println (exec! actor "t8" {:op :compliance/verify :subject "account-2" :no-spec? true} operator))

    (println "== actuation/execute-payment account-3, PIS channel, NO consent on file (-> HARD hold) ==")
    (println (exec! actor "t9" {:op :actuation/execute-payment :subject "account-3" :channel :pis} operator))

    (println "== audit ledger ==")
    (doseq [f (store/ledger db)] (println f))

    (println "== draft payment-execution records ==")
    (doseq [r (store/payment-execution-history db)] (println r))

    (println "== draft remittance-payout records ==")
    (doseq [r (store/remittance-payout-history db)] (println r))

    (println "== draft consent records ==")
    (doseq [r (store/consent-history db)] (println r))))
