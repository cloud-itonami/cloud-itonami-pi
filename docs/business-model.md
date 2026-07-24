# Open Business Blueprint: cloud-itonami-pi

This repository publishes an OSS business model for operating a Payment
Institution (PSD2 Annex I) service on itonami.cloud.

## Classification

- Repository name: `cloud-itonami-pi`
- Primary classification: PSD2 Annex I Payment Institution (licence type,
  not an ISIC code)
- Activity: money remittance, payment initiation services (PIS), account
  information services (AIS), execution of payment transactions on a
  payment account
- Served domain: general-purpose governed payment execution -- any
  legitimate payment use case, not limited to any one industry
- Original implementation context: commissioned after a payment-industry
  research project
  (`90-docs/adr/2607246000-adult-content-payment-processor-banking-jurisdiction-research.edn`)
  found this fleet had banking (`cloud-itonami-isic-6419`), credit
  (`cloud-itonami-isic-6492`) and card-processing (`cloud-itonami-isic-
  6619`) actors, but no actor covering the PSD2 sense of a Payment
  Institution

## Customer

Primary customers:

- fintech/marketplace platforms that need governed, auditable payment
  execution without building their own compliance layer
- money-remittance operators serving cross-border corridors
- open-banking (PIS/AIS) aggregators who need a governed consent +
  execution scaffold
- payment-services providers who want an auditable intake + AML/KYC +
  sanctions-screening tool instead of ad hoc spreadsheets/email

## Problem

Payment-execution SaaS and in-house tooling today are either opaque
about which legal source justifies a licensing/evidence requirement, or
willing to execute a payment/remittance without a clear, auditable AML/
sanctions/consent trail. A customer relying on a PI has no way to verify
why a payment was allowed to execute, or to prove after the fact that
execution was screened and approved properly -- and no way to verify
that a third-party payment-initiation act actually had their consent
behind it.

## Offer

Operators provide a governed payment-execution intake + execution tool:

- account intake and normalization
- per-jurisdiction AML/KYC evidence checklist + safeguarding-arrangement
  record, always citing an official source (never a fabricated
  requirement)
- sanctions screening gate on every account
- PIS/AIS consent registration, with a real spec-basis citation
- human-approved payment-execution and remittance-payout handoff (the
  actor never executes or remits alone)
- immutable audit ledger of every draft, hold, and approval

The core promise: the PaymentOps-LLM can draft and check, but it cannot
execute a payment or remit a payout unless a human operator -- who holds
the actual PSD2 (or equivalent) license and liability -- approves.

## Revenue

Operators can sell:

- per-execution / per-remittance fee
- jurisdiction-pack licensing: a maintained, spec-cited PSD2/AML
  requirement catalog for a specific country, kept current
- managed hosting: monthly subscription per tenant
- KYC/sanctions-screening add-on (integration with a real screening
  provider is the operator's responsibility)
- compliance package: audit export, retention, security review

| Package | Customer | Price shape |
|---|---|---|
| Per-execution | individual merchant/customer | flat fee per payment/remittance |
| Jurisdiction pack | payment-services provider | subscription per country covered |
| Managed tenant | fintech/marketplace platform | monthly platform fee |
| Operator enablement | new PI-licensed operator | training + certification |

## Unit Economics

Track these numbers for every operator:

- setup hours per new jurisdiction added to `pi.facts`
- LLM cost per intake/verification/screening/consent/execution operation
- KYC/sanctions-screening provider cost per account
- human-approval hours per execution/remittance
- incident and audit hours
- gross margin after infrastructure, screening-provider and support costs

## Open Participation

Anyone may:

- fork the repository
- run the demo
- deploy a self-hosted instance
- submit issues and patches
- publish an additional jurisdiction pack (with a real official
  spec-basis citation)
- create a local operator business

itonami.cloud should require certification -- including proof of the
jurisdiction's actual PSD2 (or equivalent) Payment Institution license --
before listing an operator as a trusted provider or routing customer
leads.

## Operator Trust Levels

| Level | Capability |
|---|---|
| Contributor | patches, docs, issues, examples, jurisdiction packs |
| Self-host operator | runs their own instance with no platform endorsement |
| Certified operator | listed on itonami.cloud after review, including licensing proof |
| Managed operator | may receive leads and operate customer tenants |
| Core maintainer | can approve changes to governor, security and governance |

## Marketplace Metadata

```edn
{:itonami.blueprint/id "cloud-itonami-pi"
 :itonami.blueprint/name "Payment Institution"
 :itonami.blueprint/domain :finance/payment-services
 :itonami.blueprint/license "AGPL-3.0-or-later"
 :itonami.blueprint/operator-model :certified-open-business
 :itonami.blueprint/repo "https://github.com/cloud-itonami/cloud-itonami-pi"
 :itonami.blueprint/status :public-oss}
```

## Non-Negotiables

- Do not commit real customer accounts, identification documents or
  screening results.
- Do not bypass the PIGovernor for a payment execution or remittance
  payout.
- Do not add a jurisdiction to `pi.facts` without a real, citable
  official source.
- Do not market an uncertified deployment as an itonami.cloud certified
  operator, and do not operate in a jurisdiction without the license that
  jurisdiction actually requires of a Payment Institution / money
  remitter.
- Do not expand this actor into e-money issuance, card-acquiring
  settlement mechanics, or issuer-side card program/BIN sponsorship --
  see README `Scope` for the sibling actors that own those.
