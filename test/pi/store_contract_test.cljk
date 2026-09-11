(ns pi.store-contract-test
  "The Store contract as executable tests. This actor is MemStore-only at
  this maturity stage (see README `Maturity`) -- this suite proves the
  CRUD + ledger + double-actuation-guard contract `pi.governor`/`pi.
  operation` depend on, the same discipline every sibling actor's
  `store_contract_test` establishes (there they additionally prove
  MemStore ≡ DatomicStore parity; a future DatomicStore addition to this
  repo should extend this namespace the same way, not replace it)."
  (:require [clojure.test :refer [deftest is testing]]
            [pi.store :as store]))

(deftest read-parity
  (let [s (store/seed-db)]
    (is (= "Sato Ichiro" (:holder-name (store/account s "account-1"))))
    (is (= "JPN" (:jurisdiction (store/account s "account-1"))))
    (is (= "DE89370400440532013000" (:iban (store/account s "account-1"))))
    (is (false? (:sanctions-flag-unresolved? (store/account s "account-1"))))
    (is (true? (:sanctions-flag-unresolved? (store/account s "account-4"))))
    (is (= ["account-1" "account-2" "account-3" "account-4"] (mapv :id (store/all-accounts s))))
    (is (nil? (store/compliance-of s "account-1")))
    (is (nil? (store/sanctions-screen-of s "account-1")))
    (is (nil? (store/consent-of s "account-1" :pis)))
    (is (= [] (store/ledger s)))
    (is (= [] (store/payment-execution-history s)))
    (is (= [] (store/remittance-payout-history s)))
    (is (= [] (store/consent-history s)))
    (is (zero? (store/next-payment-sequence s "JPN")))
    (is (zero? (store/next-remittance-sequence s "JPN")))
    (is (zero? (store/next-consent-sequence s "JPN" :pis)))))

(deftest write-and-ledger-parity
  (let [s (store/seed-db)]
    (testing "partial upsert merges, preserving untouched fields"
      (store/commit-record! s {:effect :account/upsert
                               :value {:id "account-1" :status :ready}})
      (is (= :ready (:status (store/account s "account-1"))))
      (is (= "Sato Ichiro" (:holder-name (store/account s "account-1"))) "name preserved"))
    (testing "compliance / sanctions-screen payloads commit and read back"
      (store/commit-record! s {:effect :compliance/set :path ["account-1"]
                               :payload {:jurisdiction "JPN" :checklist ["a" "b"]}})
      (is (= {:jurisdiction "JPN" :checklist ["a" "b"]} (store/compliance-of s "account-1")))
      (store/commit-record! s {:effect :sanctions-screen/set :path ["account-1"]
                               :payload {:account-id "account-1" :verdict :resolved}})
      (is (= {:account-id "account-1" :verdict :resolved} (store/sanctions-screen-of s "account-1"))))
    (testing "PIS consent registration drafts a record and advances the sequence"
      (store/commit-record! s {:effect :consent/mark-registered-pis :path ["account-1"]})
      (is (= :active (:status (store/consent-of s "account-1" :pis))))
      (is (= "JPN-PIS-000000" (:consent-number (store/consent-of s "account-1" :pis))))
      (is (= 1 (count (store/consent-history s))))
      (is (= 1 (store/next-consent-sequence s "JPN" :pis))))
    (testing "payment execution drafts a record and advances the sequence"
      (store/commit-record! s {:effect :account/mark-executed :path ["account-1"]})
      (is (= "JPN-PMT-000000" (get (first (store/payment-execution-history s)) "record_id")))
      (is (= "payment-execution-draft" (get (first (store/payment-execution-history s)) "kind")))
      (is (true? (:payment-executed? (store/account s "account-1"))))
      (is (= 1 (count (store/payment-execution-history s))))
      (is (= 1 (store/next-payment-sequence s "JPN"))))
    (testing "remittance payout drafts a record and advances the sequence"
      (store/commit-record! s {:effect :account/mark-remitted :path ["account-1"]})
      (is (= "JPN-REM-000000" (get (first (store/remittance-payout-history s)) "record_id")))
      (is (true? (:remittance-payout-posted? (store/account s "account-1"))))
      (is (= 1 (store/next-remittance-sequence s "JPN"))))
    (testing "ledger is append-only and order-preserving"
      (store/append-ledger! s {:op :a :disposition :commit})
      (store/append-ledger! s {:op :b :disposition :hold})
      (is (= [:commit :hold] (mapv :disposition (store/ledger s)))))))

(deftest double-actuation-guards-are-independent-per-account
  (let [s (store/seed-db)]
    (is (false? (store/account-already-executed? s "account-1")))
    (is (false? (store/account-already-remitted? s "account-1")))
    (store/commit-record! s {:effect :account/mark-executed :path ["account-1"]})
    (is (true? (store/account-already-executed? s "account-1")))
    (is (false? (store/account-already-remitted? s "account-1"))
        "executing a payment does not also mark the remittance guard")
    (is (false? (store/account-already-executed? s "account-2"))
        "the guard is per-account, not global")))
