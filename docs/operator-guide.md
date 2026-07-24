# Operator Guide

This guide is for people who want to start an open business from
`cloud-itonami-pi`.

## 1. Fork and Run

```bash
git clone https://github.com/cloud-itonami/cloud-itonami-pi
cd cloud-itonami-pi
clojure -M:dev:test
clojure -M:dev:run
```

The default demo uses synthetic accounts. Production customer accounts,
identification documents and screening results must stay outside the
repository and be injected through a store adapter.

## 2. Choose an Operating Mode

| Mode | Use when |
|---|---|
| Demo | validating the actor and governor contract |
| Self-host | one operator owns infrastructure and customer data |
| Managed tenant | an operator hosts for a fintech/marketplace platform |
| Certified operator | itonami.cloud has reviewed license, security and process controls |

## 3. Production Checklist

- confirm you (the operator) hold whatever PSD2 (or equivalent national)
  Payment Institution license the target jurisdiction requires -- this
  software does not grant or substitute for one
- replace demo data with a customer-owned store
- configure Datomic Local, kotoba-server or an equivalent durable SSoT
  (this repo is MemStore-only at this maturity stage -- see README
  `Maturity`)
- configure the LLM adapter through environment variables or a secret
  manager
- integrate a real KYC/sanctions-screening provider behind
  `pi.piadvisor`'s `:sanctions/screen` path
- integrate a real open-banking (XS2A) gateway behind `pi.piadvisor`'s
  `:consent/register-pis`/`:consent/register-ais` paths
- integrate a real payment rail behind `pi.operation`'s commit path for
  `:actuation/execute-payment`/`:actuation/remit-payout`
- implement and evidence a real safeguarding-of-funds arrangement (PSD2
  Art. 10 or equivalent) -- this software only records that one is on
  file, it does not implement or verify it
- extend `pi.facts/catalog` for every jurisdiction you serve, each entry
  citing the jurisdiction's own official payment-services supervisor as
  `:provenance`
- run `clojure -M:dev:test`
- run `clojure -M:lint`
- verify audit-ledger export
- document backup and restore
- document incident response
- get written approval for handling customer identification documents

## 4. Sales Motion

Start with a narrow offer:

1. one jurisdiction, one payment corridor
2. prove the governed AML/KYC + sanctions-screening flow
3. run one payment execution through human approval end-to-end
4. export the audit ledger for the customer's own records
5. expand to a second jurisdiction only after the first is repeatable

Avoid selling "any country, any payment type" before the jurisdiction
pack and the human-approval workflow for that jurisdiction actually
exist and have been exercised.

## 5. Certification Requirements

itonami.cloud certification should require:

- passing tests and lint on the published version
- proof of the operator's PSD2 (or equivalent) Payment Institution
  license where the jurisdiction requires one
- written data-flow diagram, including where KYC documents are stored
  and where safeguarded funds are held
- backup/restore evidence
- incident contact and response window
- proof that every execution/remittance passes through a human approval
  step (never bypassed, never auto-committed -- see README `Actuation`)
- proof that a PIS-channel execution always has a valid registered
  consent on file before it can even reach a human approver
- proof that real customer identification documents are not stored in
  Git
- customer-facing support terms

## 6. Operator Responsibilities

Operators are responsible for:

- holding the actual PSD2 (or equivalent) license a jurisdiction requires
- customer consent and lawful basis for KYC/open-banking data processing
- the real integration with each jurisdiction's payment rail and
  open-banking (XS2A) gateway
- the real safeguarding-of-funds arrangement for customer funds
- secure infrastructure and tenant isolation
- human approval workflow staffing (someone has to actually review and
  approve each execution/remittance)
- data-retention policy for identification documents
- security updates

The OSS project provides software and an operating blueprint. It does
not make an operator licensed, KYC-compliant, or legally authorized to
execute payments on anyone's behalf by itself.
