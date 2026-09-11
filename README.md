# cloud-itonami-pi

Open Business Blueprint for a **Payment Institution (PI)** in the PSD2
Annex I regulatory-license sense: money remittance, payment initiation
services (PIS), account information services (AIS), and execution of
payment transactions on a payment account. This repository publishes a
payment-institution actor as an OSS business that any qualified, licensed
operator can fork, deploy, run, improve and sell.

Built on this workspace's
[`langgraph-clj`](https://github.com/com-junkawasaki/langgraph-clj)
StateGraph runtime (portable `.cljc`, supervised superstep loop,
interrupts, in-mem checkpoints) -- the same actor pattern as every actor
in this fleet, most closely
[`cloud-itonami-isic-6419`](https://github.com/cloud-itonami/cloud-itonami-isic-6419)
(Banking Advisor ⊣ Monetary Intermediation Governor) and
[`cloud-itonami-isic-6910`](https://github.com/cloud-itonami/cloud-itonami-isic-6910)
(Registrar-LLM ⊣ RegistrarGovernor). Here it is
**PaymentOps-LLM ⊣ PIGovernor**.

> **Why an actor layer at all?** An LLM is great at drafting an
> AML/KYC evidence checklist, normalizing account intake, and checking
> whether a payment account's own IBAN actually passes its own ISO 7064
> MOD 97-10 checksum -- but it has **no notion of which jurisdiction's
> PSD2 (or equivalent) payment-services licensing regime is official, no
> license to actually execute a real payment or remit a real payout, and
> no way to know on its own whether a sanctions flag against an account
> has actually stayed unresolved, or whether a required PIS consent
> actually exists on file**. Letting it execute a payment or remit a
> payout directly invites fabricated regulatory citations, an unbalanced
> or checksum-invalid IBAN reaching a real transfer, a sanctions hit being
> quietly overlooked, and a third-party payment-initiation act with no
> customer consent behind it -- and liability, and financial-crime risk,
> for whoever runs it. This project seals the PaymentOps-LLM into a
> single node and wraps it with an independent **PIGovernor**, a human
> **approval workflow**, and an immutable **audit ledger**.

## Scope: what a Payment Institution is (and is not)

A **Payment Institution (PI)** is the PSD2 Annex I regulatory-license
category. The seven Annex I payment services are:

1. services enabling cash to be placed on, or withdrawn from, a payment account
2. execution of payment transactions (direct debits, card payments, credit transfers) on a payment account
3. execution of payment transactions where the funds are covered by a credit line for the payment service user
4. issuing of payment instruments and/or acquiring of payment transactions
5. money remittance
6. payment initiation services (PIS)
7. account information services (AIS)

This actor governs services **2, 5, 6 and 7** end-to-end (execution of
payment transactions, money remittance, PIS, AIS), and models service 1
(cash placement/withdrawal on the payment account) as part of ordinary
account intake rather than as a separate governed op at this maturity.
Service 3 (credit-line-covered execution) is a variant of service 2 this
R0 does not yet distinguish from a directly-funded execution -- an honest
gap, not a claim of coverage.

### Non-goals -- what this actor deliberately does NOT do

This fleet already has several actors whose scope overlaps conceptually
with "payment services." This actor is drawn narrowly to avoid
duplicating them:

- **Does not issue electronic money or hold e-money balances.** Issuing
  e-money is the separate **EMD2 Electronic Money Institution (EMI)**
  license category, not PSD2 Annex I -- a PI executes payment
  transactions and remits money, but does NOT itself issue e-money
  instruments or carry customer e-money balance liabilities on its own
  books. The parallel `cloud-itonami-emi` actor (built alongside this
  one) owns that institutional category. Where PSD2/EMD2 licenses are
  commonly held together in practice by the same commercial entity, this
  fleet still keeps the two actors institutionally separate, matching the
  regulatory split -- an operator running both licenses runs both actors.
- **Does not take deposits in the banking-license sense.** That is
  [`cloud-itonami-isic-6419`](https://github.com/cloud-itonami/cloud-itonami-isic-6419)
  (Community Monetary Intermediation, a deposit-taking bank/credit-union
  actor). A PI's customer funds are **safeguarded** (segregated /
  insured, PSD2 Art. 10 and its national transpositions), never lent out
  or commingled on a PI's own balance sheet the way a bank's deposits
  are -- a structurally different liability, reflected in this actor's
  evidence checklist (`safeguarding-of-funds-arrangement-record`, not a
  deposit-insurance record).
- **Does not duplicate merchant-acquiring/card-network settlement.** That
  narrower slice -- card-processing acquiring and settlement mechanics --
  is
  [`cloud-itonami-isic-6619`](https://github.com/cloud-itonami/cloud-itonami-isic-6619).
  This actor's `:actuation/execute-payment`/`:actuation/remit-payout` ops
  model the PI's own payment-account-level execution and cross-border
  remittance disbursement, not card-network acquiring/interchange/scheme
  settlement -- for that specific mechanics, see `-6619` as a sibling; do
  not reimplement it here. PSD2 Annex I service 4 (issuing payment
  instruments / acquiring) is accordingly out of scope for this actor.
- **Does not own issuer-side card program / BIN sponsorship.** That is
  the separate `cloud-itonami-card-issuing` actor (built alongside this
  one). A PI executing payment transactions may sit downstream of a card
  program, but does not itself sponsor a BIN or issue card programs.

The result: this actor is the fleet's first and only actor covering the
PSD2 Annex I "general payment-account execution + PIS/AIS + money
remittance" service set -- the gap identified by
`90-docs/adr/2607246000-adult-content-payment-processor-banking-jurisdiction-research.edn`.
It is **general-purpose** (any legitimate payment use case), not
adult-industry-specific -- that research is the originating motivation
for filling this gap, not this actor's scope.

### Actuation

**Executing a real payment transaction or remitting a real money
transfer is never autonomous, at any phase, by construction.** Two
independent layers enforce this (`pi.governor`'s `:actuation/execute-
payment`/`:actuation/remit-payout` high-stakes gate and `pi.phase`'s
phase table, which never puts either op in any phase's `:auto` set) --
see `pi.phase`'s docstring and `test/pi/phase_test.cljk`'s
`actuation-never-auto-at-any-phase`. The actor may draft, check and
recommend; a human PI operator is always the one who actually executes a
payment or remits a payout. This actor has TWO actuation events
(executing a payment, posting a remittance payout), both POSITIVE
(committing a real record), matching this fleet's majority actuation
shape.

## The core contract

```
account intake + jurisdiction facts (pi.facts, spec-cited)
        |
        v
   ┌──────────────┐   proposal      ┌───────────────────────┐
   │ PaymentOps-  │ ─────────────▶ │ PIGovernor:            │  (independent system)
   │ LLM (sealed) │  + citations    │ spec-basis · evidence ·│
   └──────────────┘         commit ◀────┼──────────▶ hold │ IBAN checksum ·
                                 │             │           │ sanctions-flag ·
                           record + ledger  escalate ─▶ human   PIS-consent-missing ·
                                             (ALWAYS for         already-executed/
                                              :actuation/execute-        -remitted
                                              payment /
                                              :actuation/remit-payout)
```

**The PaymentOps-LLM never executes a payment or remits a payout the
PIGovernor would reject, and never does so without a human sign-off.**
Hard violations (fabricated regulatory requirements; unsupported
evidence; an IBAN checksum failure; an unresolved sanctions flag; a
missing PIS consent for a PIS-channel execution; a double execution or
remittance) force **hold** and *cannot* be approved past; a clean
execution/remittance proposal still always routes to a human.

## Run

```bash
kbb -M:dev:run     # walk one clean dual-actuation lifecycle + HARD-hold cases through the actor
kbb -M:dev:test    # governor contract · phase invariants · store parity · registry conformance · facts coverage
kbb -M:lint        # clj-kondo (errors fail; CI mirrors this)
```

## Open business

This repository is not only source code. It is a public, forkable
business model:

| Layer | What is open |
|---|---|
| OSS core | Actor runtime, PIGovernor, payment-execution/remittance-payout/PIS/AIS-consent draft records, audit ledger |
| Business blueprint | Customer, offer, pricing, unit economics, sales motion |
| Operator playbook | How to fork, license, deploy and support the service in a jurisdiction |
| Trust controls | Governance, security reporting, actuation invariant, audit requirements |

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md) to start this as an
open business on itonami.cloud, and
[`docs/adr/0001-architecture.md`](docs/adr/0001-architecture.md) for the
full architecture and decision record.

## Layout

| File | Role |
|---|---|
| `src/pi/store.cljk` | **Store** protocol -- `MemStore` (this actor is MemStore-only at this maturity stage, see `Maturity` below) + append-only audit ledger + separate payment-execution/remittance-payout/PIS-AIS-consent history |
| `src/pi/registry.cljk` | Payment-execution + remittance-payout + PIS/AIS-consent draft records, plus `iban-checksum-invalid?` -- a REAL, independently-implemented ISO 7064 MOD 97-10 check (the same algorithm `cloud-itonami-isic-6419`'s `banking.registry` establishes, re-implemented here so this standalone repo has no compile-time dependency on a sibling actor's internals) |
| `src/pi/facts.cljk` | Per-jurisdiction PSD2/payment-services licensing + AML/KYC catalog with an official spec-basis citation per entry, honest coverage reporting |
| `src/pi/piadvisor.cljk` | **PaymentOps-LLM** -- `mock-advisor` ‖ `llm-advisor`; intake/compliance-verification/sanctions-screening/PIS-AIS-consent/payment-execution/remittance-payout proposals |
| `src/pi/governor.cljk` | **PIGovernor** -- effect-matches-op · spec-basis · evidence-incomplete · IBAN checksum (independent recompute) · sanctions flag (unconditional) · PIS-consent-missing · already-executed/already-remitted guards · confidence/actuation gate |
| `src/pi/phase.cljk` | **Phase 0→3** -- read-only → assisted intake → assisted verify → supervised (both payment execution and remittance payout always human; account intake is the ONLY auto-eligible op, no direct capital risk) |
| `src/pi/operation.cljk` | **OperationActor** -- langgraph-clj StateGraph |
| `src/pi/sim.cljk` | demo driver |
| `test/pi/*_test.clj` | governor contract · phase invariants · store contract · registry conformance · facts coverage · real-LLM advisor (mock-model) |

## Business-process coverage (honest)

| Covered | Not covered (out of scope for this R0) |
|---|---|
| Account intake (`:account/intake`) | Cash placement/withdrawal as a separately-governed op distinct from ordinary intake (Annex I service 1) |
| AML/KYC compliance assessment, HARD-gated on an official spec-basis citation (`:compliance/verify`) | Credit-line-covered execution as a distinct op from a directly-funded execution (Annex I service 3) |
| Sanctions screening, evaluated unconditionally (`:sanctions/screen`) | Issuing payment instruments / acquiring transactions (Annex I service 4 -- see `cloud-itonami-isic-6619` for card-acquiring mechanics, `cloud-itonami-card-issuing` for issuer-side programs) |
| PIS consent registration (`:consent/register-pis`) | Electronic money issuance / e-money balance custody (see `cloud-itonami-emi`) |
| AIS consent registration (`:consent/register-ais`) | Real open-banking (XS2A) gateway integration, real payment-rail integration -- operator responsibility |
| Payment execution, HARD-gated on full evidence, IBAN checksum, sanctions resolution, and (for PIS-channel) a registered consent, plus a double-execution guard (`:actuation/execute-payment`) | |
| Money-remittance payout, HARD-gated on full evidence, beneficiary IBAN checksum, sanctions resolution, plus a double-payout guard (`:actuation/remit-payout`) | |
| Immutable audit ledger for every intake/verification/screening/consent/execution/payout decision | |

## Jurisdiction coverage (honest)

`pi.facts/coverage` reports how many requested jurisdictions actually
have an official spec-basis in `pi.facts/catalog` -- currently 9 seeded
(DEU, FRA, IRL, NLD, LTU, GBR, JPN, SGP, USA-NY, BRA) out of ~194
jurisdictions worldwide. This is a starting catalog to prove the governor
contract end-to-end, not a claim of global coverage. Adding a
jurisdiction is additive: one map entry in `pi.facts/catalog`, citing a
real official source -- never fabricate a jurisdiction's requirements to
make coverage look bigger.

## Maturity

This actor is published at the same maturity level as
[`cloud-itonami-isic-6910`](https://github.com/cloud-itonami/cloud-itonami-isic-6910)'s
initial R0: `PaymentOps-LLM` + `PIGovernor` run as real, tested code (see
`Run` above) -- a real actor loop (advisor/governor/phase/registry/store/
operation), **MemStore-only** persistence (no `DatomicStore`/`langchain.
db`/real open-banking or payment-rail integration wired up yet), every
destructive/regulated action hard-gated behind human approval, no
Datomic/kotoba-server backend yet. Extending to a `DatomicStore`
(`langchain.db`-backed) is the natural next seam, following the exact
pattern `cloud-itonami-isic-6419`/`-6910` already proved out.

## License

Code and implementation templates are AGPL-3.0-or-later.
