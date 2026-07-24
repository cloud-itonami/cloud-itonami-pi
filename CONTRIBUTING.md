# Contributing

`cloud-itonami-pi` accepts contributions to the OSS actor, governor
tests, documentation, jurisdiction packs and open business blueprint.

## Development

```bash
clojure -M:dev:test
clojure -M:lint
```

Keep changes small and include tests for governor, phase, registry or
facts-coverage behavior.

## Rules

- Do not commit real customer accounts, credentials, identification
  documents or screening results.
- Keep payment execution and remittance payout behind PIGovernor AND the
  phase table -- never remove `:actuation/execute-payment`/
  `:actuation/remit-payout` from a governor hard-check or add either to a
  phase's `:auto` set.
- Treat this as a high-risk domain: add tests for spec-basis, evidence
  completeness, IBAN checksum, sanctions, PIS-consent and audit logging
  with every change.
- A new jurisdiction entry in `pi.facts/catalog` MUST cite a real
  official source (`:provenance`) -- do not add a placeholder.
- Do not expand this actor's scope into e-money issuance, card-acquiring
  settlement mechanics, or issuer-side card program/BIN sponsorship --
  see README `Scope` for the sibling actors that own those.
- Document any new business-model or operator assumption in `docs/`.

## Pull Requests

PRs should describe:

- what behavior changed
- which governor or phase invariant is affected
- how it was tested
- whether operator or certification docs need updates
