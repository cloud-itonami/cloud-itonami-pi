# ADR-0001: cloud-itonami-pi — PaymentOps-LLM を封じ込めた知能ノードとする Payment Institution (PSD2 Annex I) アクター設計

- Status: Accepted (2026-07-25)
- 関連: `cloud-itonami-isic-6419`（Banking Advisor ⊣ Monetary Intermediation
  Governor、IBAN/ISO 7064 MOD 97-10 の先行実装）、`cloud-itonami-isic-6910`
  ADR-0001（RegistrarGovernor パターン、本 ADR が目標とする R0 成熟度の
  手本。特に Addendum 9/12/14/15 の governance-bypass バグクラス）、
  `cloud-itonami-isic-6619`（カード処理/acquiring、本アクターとスコープが
  隣接するがオーバーラップしない sibling）、並行して起票される
  `cloud-itonami-emi`（Electronic Money Institution アクター）・
  `cloud-itonami-card-issuing`（issuer-side カードプログラム アクター）、
  langgraph-clj ADR-0001（Pregel superstep + interrupt + checkpoint）、
  `90-docs/adr/2607246000-adult-content-payment-processor-banking-jurisdiction-research.edn`
  （本アクターを起票する契機となった決済業界調査 -- cloud-itonami に
  banking(6419)/credit(6492)/card-processing(6619) アクターは存在するが、
  PSD2 の意味での PI（Payment Institution）アクターが存在しないという
  ギャップを発見）。

## 課題

決済サービス（money remittance / payment initiation / account
information / execution of payment transactions）をグローバルに提供する
Payment Institution 業務を governed actor として提供するには、次の
異なる性質の判断が必要になる。

1. **法域ライセンス要件の正しさ** -- PSD2（またはその同等の各国法制）の
   ライセンス要件・必要書類・safeguarding 要件が、公式ソースに基づいて
   いるか。
2. **AML/KYC・制裁チェック** -- 口座保有者が制裁・PEP リストに一致して
   いないか。
3. **PIS 同意の存否** -- 第三者の ASPSP（他行）に対する決済指図伝達
   （PIS）を行う場合、その法的根拠となる顧客の明示的同意が実際に登録
   されているか。
4. **実アクチュエーション** -- 実際に決済を実行し、実際に送金を実行する
   という、後戻りのできない実世界の行為。

LLM はこれらのいずれについても、真正性の判断根拠・法的権限・実行責任を
持たない。したがって設計課題は「LLM で決済実務を回す」ことではなく、
**「LLM を信頼境界の内側に封じ込め、法域ライセンス要件の真正性・AML/
制裁・PIS 同意・監査・人間承認の層をどう被せ、かつ実アクチュエーション
を構造的に人間専用に固定するか」**である。

加えて、この actor をどうスコープするかという設計課題があった:
cloud-itonami フリートには既に banking（`-isic-6419`、預金取扱）・
credit（`-isic-6492`）・card-processing（`-isic-6619`、カード
acquiring/決済処理）のアクターが存在するが、**PSD2 Annex I の意味での
PI（money remittance・PIS・AIS・payment account 上の決済実行）を扱う
アクターは存在しなかった** -- `-isic-6619` はカード acquiring/決済処理の
狭いスライスを扱うのみで、より広い PI サービス集合（特に PIS/AIS という
オープンバンキング特有の概念）をカバーしない。

## 決定

### 1. PaymentOps-LLM は最下層の1ノードに封じ込め、直接決済実行/送金させない

`pi.piadvisor` は intake 正規化・法域要件チェックリスト・制裁
スクリーニング・PIS/AIS 同意登録・決済実行提案・送金実行提案の6種類の
proposal だけを返す。どの proposal も SSoT への書き込みや実際の決済
実行/送金実行を直接行わない。

### 2. OperationActor = langgraph-clj StateGraph、1 run = 1 決済操作

`pi.operation/build` は本フリート共通の同型 StateGraph（intake → advise
→ govern → decide → commit | hold | request-approval）。1回の graph run
が1つの決済操作に対応し、無限の内部ループを持たない。

### 3. PIGovernor は PaymentOps-LLM と別系統

`pi.governor` は effect-matches-op・spec-basis・evidence-incomplete・
IBAN checksum（独立再計算）・sanctions-flag（無条件評価）・
pis-consent-missing・already-executed/already-remitted の7つの HARD
チェック（人間による上書き不可）+ confidence-floor・actuation-gate の
SOFT チェックを持つ。

**本 ADR は `cloud-itonami-isic-6910` の ADR-0001 が Addendum 9/12/14/15
で実機再現の末に発見・修正した governance-bypass バグクラス
（post-actuation-write の無検閲書き換え、advisor 自己申告 `:effect` の
未検証、変更許可フィールドの無制限、唯一の自動commit op の無制限）を
**最初から設計に織り込んでいる**。特に `op->effect`（effect-mismatch
チェック）は 6910 が Addendum 12 で事後的に追加したのと同型のテーブルを
R0 の時点で持つ -- 同じ穴を再発見するのではなく、既知のバグクラスとして
最初から塞ぐ。

### 4. 実アクチュエーションは構造的に常に人間専用（2層で独立に強制）

`pi.governor` の actuation gate（`:stake :actuation/execute-payment`/
`:actuation/remit-payout` は常に escalate）と `pi.phase` のフェーズ表
（両opはどのフェーズの `:auto` にも含まれない）の**両方**が、実際の
決済実行・実際の送金実行を自動化しない。片方の実装ミスがもう片方で
吸収される二重の設計にした。

### 5. IBAN/ISO 7064 MOD 97-10 の spec 数学は cloud-itonami-isic-6419 と
同じ真正アルゴリズムを、依存ではなく再実装

`pi.registry/iban-checksum-invalid?` は `cloud-itonami-isic-6419` の
`banking.registry` が確立した ISO 7064 MOD 97-10 の同じ真正
アルゴリズムを、**本リポジトリ内で独立に再実装**した（他アクターへの
compile-time 依存を持たせない、standalone/forkable actor という
このフリートの設計原則に従う）。同一スペックの2番目の独立実装であり、
"最初の実装" という主張はしない。

### 6. PIS/AIS 同意登録を独立した governed write として扱う

`:consent/register-pis`/`:consent/register-ais` は実アクチュエーション
ではない（資金は動かない、第三者口座の読み取りも発生しない）が、
`pi.facts` への spec-basis 引用を要求する governed write として実装
した。PIS チャネルでの `:actuation/execute-payment` は、リクエストが
`:channel :pis` を宣言する場合に限り、`pi.store/consent-of` に有効な
同意記録があることを governor が独立に検証する（`pis-consent-missing`
HARD check）。PSD2 における PIS の法的根拠そのものが顧客の明示的同意で
あるため、この検証は「他のチェックより軽い付随条件」ではなく、
**そもそも法的根拠が存在するかの検証**である。

direct チャネル（PI 自身の顧客が PI 自身が保有する決済口座上で実行する
決済）はこの検証の対象外 -- 第三者 ASPSP への XS2A アクセスを伴わない
ため、PIS 固有の同意フレームワークは適用されない。

### 7. スコープ境界: EMI・card-issuing・card-processing との重複回避

- **e-money の発行/保有はしない。** PSD2 Annex I は PI のサービスであり、
  EMD2 の e-money 発行は制度上別のライセンス（EMI）である。並行して
  起票される `cloud-itonami-emi` がその責務を持つ。
- **銀行免許の意味での預金は取らない。** `cloud-itonami-isic-6419` の
  責務。PI の顧客資金は safeguarding（分別管理/保険付保）の対象であり、
  銀行の預金とは法的性質が異なる -- 本アクターの evidence checklist に
  `safeguarding-of-funds-arrangement-record` という 6419 に無い項目が
  あるのはこのため。
- **カード acquiring/決済処理の具体的メカニクスは複製しない。**
  `cloud-itonami-isic-6619` の責務。本アクターの `:actuation/execute-
  payment`/`:actuation/remit-payout` は PI 自身の決済口座レベルの実行・
  越境送金であり、カードネットワークの acquiring/interchange/scheme
  決済ではない。
- **issuer-side のカードプログラム/BIN スポンサーシップは持たない。**
  並行して起票される `cloud-itonami-card-issuing` の責務。

### 8. 配置は cloud-itonami org 直下に新規 repo として発行

既存の `cloud-itonami-{ISIC}` 系列と同じ形で、KYC/custody/settlement の
liability を単一 vendor に集中させず、OSS actor を各法域の licensed
operator（gftdcojp 自身を含む）が自己運用し、各自の法域免許の下で
liability を持つ構造にする。PI は ISIC コードではなくライセンス種別
（PSD2 の意味での Payment Institution）で分類されるため、repo 名は
`cloud-itonami-isic-XXXX` ではなく `cloud-itonami-pi` とする。

## 帰結

- (+) 既存フリートに欠けていた PSD2 Annex I の意味での PI アクターの
  ギャップを埋め、`-isic-6419`（銀行）・`-isic-6619`（カード処理）・
  `-emi`（EMI、並行起票）・`-card-issuing`（issuer-side、並行起票）との
  責務境界を明文化した。
- (+) 実アクチュエーション不変条件（governor + phase の2層）は
  `test/pi/phase_test.cljk` の `actuation-never-auto-at-any-phase` で
  リグレッションを機械的に検出できる。
- (+) `cloud-itonami-isic-6910` の ADR-0001 が15の Addendum を経て
  発見した governance-bypass バグクラス（特に effect-mismatch）を、
  本アクターは R0 の時点で設計に織り込んでいる。
- (-) 本 R0 は9法域（DEU/FRA/IRL/NLD/LTU/GBR/JPN/SGP/USA-NY/BRA）のみ
  spec-basis を持つ。~194法域のうち、大半は未カバーであり、正直に
  `pi.facts/coverage` で報告する。
- (-) `MemStore` のみで、Datomic/kotoba-server backend への接続は未実装
  （`cloud-itonami-isic-6419`/`-6910` が既に実証した `:db-api` 駆動
  パターンをそのまま適用できる見込みだが、本 ADR の対象外）。
- (-) 実際のオープンバンキング(XS2A)ゲートウェイ統合・実際の決済レール
  統合・実際のKYC/制裁スクリーニングプロバイダ統合は、この OSS actor の
  対象外（各 operator の責任）。
- (-) PSD2 Annex I service 1（口座への現金入出金）を独立した governed
  op として区別していない（intake の一部として扱う）。service 3
  （credit-line-covered execution）も service 2 から未分離。いずれも
  正直な既知ギャップとして記録する。

## 代替案と不採用理由

| 案 | 採否 | 理由 |
|---|---|---|
| `cloud-itonami-isic-6619` を拡張して PI サービス全般を扱う | ❌ | 6619 はカード acquiring/決済処理の具体的メカニクスに特化しており、PIS/AIS のようなオープンバンキング固有の概念や money remittance を無理に押し込むと both のスコープが曖昧になる。sibling として分離し、cross-reference する方が責務が明確 |
| PI と EMI を単一アクターとして統合する | ❌ | PSD2 と EMD2 は制度上別のライセンス category であり、e-money 発行はPIの権限外。統合すると「PIは e-money を発行できない」という規制上の事実をコード上で表現できなくなる |
| `cloud-itonami` 本体（gftdcojp 自社業務基盤）に新規 lane として追加 | ❌ | `cloud-itonami` の activity/decision/effect/audit モデルは gftdcojp *自社* 業務用であり、顧客の KYC/custody/決済実行を扱う規制対応サービスとは設計前提が異なる（`-isic-6910` ADR-0001 と同じ理由） |
| 全法域のライセンス要件を一度に網羅しようとする | ❌ | `pi.facts` の "never fabricate" 規律に反する。9法域の正直なスタートカタログとし、追加は1エントリ=1本物の公式ソース引用を原則とする |

## Closing note (2026-07-25)

本アクターは R0 として、advisor/governor/phase/registry/store/operation
の実際に動く actor loop、MemStore-only の永続化、全ての破壊的/規制対象
アクションを人間承認でゲートした状態で公開する。`cloud-itonami-isic-
6910` が15回の反復（Addendum 1-15）を経て到達した governance-bypass
対策のいくつか（特に effect-mismatch チェック）は、本アクターでは
最初から設計に含めている -- 同じ穴を再発見するコストを払わない。

正直に残っている既知のギャップ:

1. 法域カバレッジ 9/194（`pi.facts/coverage`）。
2. `DatomicStore` backend は未実装（MemStore のみ）。
3. Annex I service 1（現金入出金）・service 3（credit-line-covered
   execution）を独立 op として区別していない。
4. 実オープンバンキング(XS2A)・実決済レール・実KYC/制裁スクリーニング
   プロバイダ統合は対象外（operator の責務、設計通り）。
