# Governance

`cloud-itonami-pi` is an OSS open-business blueprint for a Payment
Institution (PSD2 Annex I). Governance covers both code and the operator
model.

## Maintainers

Maintainers may merge changes that preserve these invariants:

- PaymentOps-LLM cannot directly execute a payment or remit a payout.
- PIGovernor remains independent of the advisor.
- hard governor violations (fabricated spec-basis, incomplete evidence,
  an invalid IBAN checksum, an unresolved sanctions flag, a missing PIS
  consent, a double execution/remittance) cannot be overridden by human
  approval.
- `:actuation/execute-payment` and `:actuation/remit-payout` are never a
  member of any phase's `:auto` set.
- every commit, hold and approval path is auditable.
- real customer identification documents, screening results and
  payment-account details stay outside Git.
- no jurisdiction is added to `pi.facts` without a real, citable official
  source.
- this actor does not expand into e-money issuance (see `cloud-itonami-
  emi`), card-acquiring settlement mechanics (see `cloud-itonami-isic-
  6619`), or issuer-side card program/BIN sponsorship (see
  `cloud-itonami-card-issuing`) -- see README `Scope`.

## Decision Records

Architecture decisions live in `docs/adr/`. Changes to the trust model,
storage contract, actuation invariant, public business model, operator
certification or license should add or update an ADR.
