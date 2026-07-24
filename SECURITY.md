# Security Policy

This project handles payment-account onboarding workflows, including
customer identification, AML/KYC evidence, sanctions-screening results
and PSD2 open-banking (PIS/AIS) consent records. Treat vulnerabilities as
potentially high impact even when the demo data is synthetic.

## Do Not Disclose Publicly

Report privately before opening public issues for:

- credential exposure
- real customer, account or identification-document exposure
- authorization bypass
- PIGovernor bypass
- a path that lets `:actuation/execute-payment` or `:actuation/remit-
  payout` auto-commit at any phase
- a path that lets a PIS-channel execution proceed without a valid
  registered consent
- audit-ledger tampering
- tenant isolation failures

## Reporting

Use GitHub private vulnerability reporting when available for the
repository. If that is unavailable, contact the repository maintainers
through the cloud-itonami organization before publishing details.

Include:

- affected commit or version
- reproduction steps
- expected and actual behavior
- impact on customer data, governor enforcement, actuation invariant or
  audit logging
- suggested fix, if known

## Production Guidance

- Store secrets outside Git.
- Keep real customer/account data outside this repository.
- Run governor and phase tests before deployment.
- Export and review audit logs regularly.
- Use least privilege for operators and service accounts.
- Never wire `:actuation/execute-payment` or `:actuation/remit-payout` to
  run without a human approval step, regardless of confidence or phase.
- Safeguard customer funds per PSD2 Art. 10 (or the equivalent national
  transposition) -- this software does not implement or verify the
  safeguarding arrangement itself; it only records that one is on file.
