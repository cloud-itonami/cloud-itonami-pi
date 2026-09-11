(ns pi.facts
  "Per-jurisdiction Payment Institution (PSD2 Annex I / equivalent national
  regime) licensing + AML/KYC catalog -- the G2-style spec-basis table
  `pi.governor` checks every `:compliance/verify`, `:consent/register-pis`,
  `:consent/register-ais`, `:actuation/execute-payment` and
  `:actuation/remit-payout` proposal against ('did the advisor cite an
  OFFICIAL public source for this jurisdiction's payment-services regime,
  or did it invent one?').

  A Payment Institution (PI) is the PSD2 Annex I regulatory-license
  category -- money remittance, payment initiation services (PIS), account
  information services (AIS) and execution of payment transactions on a
  payment account -- as distinct from an Electronic Money Institution (EMI,
  which additionally issues/holds e-money balances, see README `Scope`) and
  from a deposit-taking bank (ISIC 6419) or a card-acquiring/settlement
  processor (ISIC 6619). Some jurisdictions below regulate this activity
  under a PSD2-transposition law (the EEA member states), some under a
  distinct national money-transmission/funds-settlement statute (JPN, SGP,
  USA-NY) -- both are real, citable spec-bases for the SAME underlying
  activity class, and this catalog does not pretend they are the same
  instrument.

  Coverage is reported HONESTLY (see `coverage`), the same discipline
  every sibling actor's `facts` namespace uses: a jurisdiction not in this
  table has NO spec-basis, full stop -- the advisor must not fabricate
  one, and the governor holds if it tries.

  Seed values are drawn from each jurisdiction's official payment-services
  supervisor and its PSD2-transposition (or equivalent) statute (see
  `:provenance`); they are a STARTING catalog, not a from-scratch survey of
  all ~194 jurisdictions. Extending coverage is additive: add one map to
  `catalog`, cite a real source, done -- never invent a jurisdiction's
  requirements to make coverage look bigger.")

(def catalog
  "iso3 (or an exemplar-suffixed key for a federal jurisdiction, matching
  this fleet's `USA-DE`/`USA-NY`-style convention) -> requirement map.
  `:required-evidence` mirrors the generic identity-verification/source-
  of-funds/safeguarding-arrangement/sanctions-screening evidence set every
  sibling actor's evidence checklist submits in some form -- with one
  PI-specific addition, `safeguarding-of-funds-arrangement-record`, which
  has no analog in `cloud-itonami-isic-6419`'s banking catalog: a PSD2-style
  Payment Institution does NOT take deposits (it is not a bank), so the
  law instead requires customer funds received for execution to be
  SAFEGUARDED (segregated / insured) rather than balance-sheet-commingled
  -- PSD2 Art. 10 and its national transpositions all require this in some
  form. `:legal-basis` / `:owner-authority` / `:provenance` are the G2
  citation the governor requires before any `:compliance/verify`,
  `:consent/register-pis`, `:consent/register-ais`,
  `:actuation/execute-payment` or `:actuation/remit-payout` proposal can
  commit."
  {"DEU" {:name "Germany"
          :owner-authority "Bundesanstalt für Finanzdienstleistungsaufsicht (BaFin)"
          :legal-basis "Zahlungsdiensteaufsichtsgesetz (ZAG) -- transposing Directive (EU) 2015/2366 (PSD2)"
          :national-spec "BaFin Zahlungsdienste-Erlaubnisverfahren (ZAG licensing)"
          :provenance "https://www.bafin.de/"
          :required-evidence ["identity-verification-record"
                              "source-of-funds-record"
                              "safeguarding-of-funds-arrangement-record"
                              "sanctions-screening-record"]}
   "FRA" {:name "France"
          :owner-authority "Autorité de contrôle prudentiel et de résolution (ACPR)"
          :legal-basis "Code monétaire et financier, art. L. 522-1 et s. -- transposing PSD2 (EU) 2015/2366"
          :national-spec "ACPR agrément établissement de paiement"
          :provenance "https://acpr.banque-france.fr/"
          :required-evidence ["identity-verification-record"
                              "source-of-funds-record"
                              "safeguarding-of-funds-arrangement-record"
                              "sanctions-screening-record"]}
   "IRL" {:name "Ireland"
          :owner-authority "Central Bank of Ireland"
          :legal-basis "European Union (Payment Services) Regulations 2018 (S.I. No. 6 of 2018) -- transposing PSD2 (EU) 2015/2366"
          :national-spec "Central Bank of Ireland payment institution authorisation"
          :provenance "https://www.centralbank.ie/"
          :required-evidence ["identity-verification-record"
                              "source-of-funds-record"
                              "safeguarding-of-funds-arrangement-record"
                              "sanctions-screening-record"]}
   "NLD" {:name "Netherlands"
          :owner-authority "De Nederlandsche Bank (DNB)"
          :legal-basis "Wet op het financieel toezicht (Wft), Deel 2 -- transposing PSD2 (EU) 2015/2366"
          :national-spec "DNB vergunning betaaldienstverlener"
          :provenance "https://www.dnb.nl/"
          :required-evidence ["identity-verification-record"
                              "source-of-funds-record"
                              "safeguarding-of-funds-arrangement-record"
                              "sanctions-screening-record"]}
   "LTU" {:name "Lithuania"
          :owner-authority "Lietuvos bankas (Bank of Lithuania)"
          :legal-basis "Mokėjimų įstatymas (Law on Payments) -- transposing PSD2 (EU) 2015/2366"
          :national-spec "Lietuvos bankas payment institution licence"
          :provenance "https://www.lb.lt/"
          :required-evidence ["identity-verification-record"
                              "source-of-funds-record"
                              "safeguarding-of-funds-arrangement-record"
                              "sanctions-screening-record"]}
   "GBR" {:name "United Kingdom"
          :owner-authority "Financial Conduct Authority (FCA)"
          :legal-basis "Payment Services Regulations 2017 (PSRs 2017) -- UK's retained/onshored transposition of PSD2 (EU) 2015/2366"
          :national-spec "FCA authorised/small payment institution register"
          :provenance "https://www.fca.org.uk/"
          :required-evidence ["identity-verification-record"
                              "source-of-funds-record"
                              "safeguarding-of-funds-arrangement-record"
                              "sanctions-screening-record"]}
   "JPN" {:name "Japan"
          :owner-authority "金融庁 (Financial Services Agency, FSA)"
          :legal-basis "資金決済に関する法律 (Payment Services Act) -- 資金移動業 (Funds Transfer Service Provider) regime"
          :national-spec "資金移動業者登録制度 (Funds Transfer Service Provider registration)"
          :provenance "https://www.fsa.go.jp/"
          :notes "Japan has no PSD2-style directive; money remittance/funds-transfer is licensed under its own national Payment Services Act rather than a PSD2 transposition -- a different statutory instrument for the same activity class."
          :required-evidence ["identity-verification-record"
                              "source-of-funds-record"
                              "safeguarding-of-funds-arrangement-record"
                              "sanctions-screening-record"]}
   "SGP" {:name "Singapore"
          :owner-authority "Monetary Authority of Singapore (MAS)"
          :legal-basis "Payment Services Act 2019 (PS Act)"
          :national-spec "MAS major payment institution / standard payment institution licence"
          :provenance "https://www.mas.gov.sg/"
          :required-evidence ["identity-verification-record"
                              "source-of-funds-record"
                              "safeguarding-of-funds-arrangement-record"
                              "sanctions-screening-record"]}
   "USA-NY" {:name "United States -- New York (exemplar; federalism note below)"
             :owner-authority "New York State Department of Financial Services (NYDFS)"
             :legal-basis "New York Banking Law Article 13-B (Money Transmitters) / 23 NYCRR Part 200"
             :national-spec "NYDFS money transmitter license"
             :provenance "https://www.dfs.ny.gov/"
             :notes "No federal PI/money-transmission licence in the US -- money transmission (the closest US analog to a PSD2 Payment Institution) is licensed per-state; New York is an exemplar, not a national authority, the same USA-DE/USA-NY-exemplar convention this fleet already uses (cloud-itonami-isic-6910)."
             :required-evidence ["identity-verification-record"
                                 "source-of-funds-record"
                                 "safeguarding-of-funds-arrangement-record"
                                 "sanctions-screening-record"]}
   "BRA" {:name "Brazil"
          :owner-authority "Banco Central do Brasil (BCB)"
          :legal-basis "Lei nº 12.865/2013 (arranjos de pagamento e instituições de pagamento) + Resoluções BCB"
          :national-spec "BCB autorização de instituição de pagamento"
          :provenance "https://www.bcb.gov.br/"
          :required-evidence ["identity-verification-record"
                              "source-of-funds-record"
                              "safeguarding-of-funds-arrangement-record"
                              "sanctions-screening-record"]}})

(defn spec-basis
  "The jurisdiction's requirement map, or nil -- nil means NO spec-basis,
  and the governor must hold any proposal that tries to verify compliance,
  register a consent, execute a payment or remit a payout on it."
  [iso3]
  (get catalog iso3))

(defn coverage
  "Honest coverage report: how many of the requested jurisdictions actually
  have a spec-basis entry. Never report a missing jurisdiction as covered."
  ([] (coverage (keys catalog)))
  ([iso3s]
   (let [have (filter catalog iso3s)
         missing (remove catalog iso3s)]
     {:requested (count iso3s)
      :covered (count have)
      :covered-jurisdictions (vec (sort have))
      :missing-jurisdictions (vec (sort missing))
      :note (str "cloud-itonami-pi R0: " (count catalog)
                 " jurisdictions seeded with an official spec-basis. "
                 "This is a starting catalog, not a survey of all ~194 "
                 "jurisdictions -- extend `pi.facts/catalog`, never "
                 "fabricate a jurisdiction's requirements.")})))

(defn required-evidence-satisfied?
  "Does `submitted` (a set/coll of evidence keywords or strings) satisfy
  every evidence item listed for `iso3`? Missing spec-basis -> never
  satisfied."
  [iso3 submitted]
  (when-let [{:keys [required-evidence]} (spec-basis iso3)]
    (let [need (count required-evidence)
          have (count (filter (set submitted) required-evidence))]
      (= need have))))

(defn evidence-checklist [iso3]
  (:required-evidence (spec-basis iso3) []))
