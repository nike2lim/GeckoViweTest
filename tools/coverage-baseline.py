#!/usr/bin/env python3
"""
커버리지 베이스라인 생성·대조 도구 (후속 과제 F-4)

베이스라인이 산문(coverage-report.md)에만 있으면 각 단계가 그 값을 신뢰할 수 없어
매번 `clean jvmCoverageReport --rerun-tasks`로 재산출해야 한다(배치 02에서 6번 반복됐다).
이 도구는 베이스라인을 기계가 읽는 파일로 고정해 그 반복을 없앤다.

사용법
  # 현재 측정치를 새 베이스라인으로 기록 (배치 종료 시 coverage-reporter가 실행)
  python3 coverage-baseline.py generate --out pipeline/coverage-baseline.json

  # 현재 측정치를 베이스라인과 대조 (다음 배치의 각 단계가 실행)
  python3 coverage-baseline.py check

종료 코드: 0 = PASS, 1 = FAIL(회귀 또는 제외 범위 확대), 2 = 입력 오류
"""

import argparse
import glob
import json
import os
import re
import sys
import xml.etree.ElementTree as ET

METRICS = ["LINE", "BRANCH", "CLASS", "INSTRUCTION", "METHOD", "COMPLEXITY"]

DEFAULT_XML = "app/build/reports/jacoco/jvmCoverageReport/jvmCoverageReport.xml"
DEFAULT_TESTS = "app/build/test-results/testDebugUnitTest"
DEFAULT_GRADLE = "app/build.gradle.kts"
DEFAULT_BASELINE = "pipeline/coverage-baseline.json"


def counters(node):
    out = {}
    for c in node.findall("counter"):
        cov = int(c.get("covered"))
        missed = int(c.get("missed"))
        out[c.get("type")] = [cov, cov + missed]
    return out


def read_xml(path):
    if not os.path.exists(path):
        print(f"[오류] 커버리지 XML이 없다: {path}\n"
              f"       먼저 실행하라: JAVA_HOME=... ./gradlew clean jvmCoverageReport --rerun-tasks",
              file=sys.stderr)
        sys.exit(2)
    try:
        root = ET.parse(path).getroot()
    except OSError as e:
        print(f"[오류] 커버리지 XML을 열 수 없다: {path}\n       {e}", file=sys.stderr)
        sys.exit(2)
    except ET.ParseError as e:
        print(f"[오류] 커버리지 XML이 손상됐다: {path}\n       {e}", file=sys.stderr)
        sys.exit(2)
    totals = {k: v for k, v in counters(root).items() if k in METRICS}
    classes = {}
    for pkg in root.findall("package"):
        for cl in pkg.findall("class"):
            cc = {k: v for k, v in counters(cl).items() if k in METRICS}
            if cc:  # 코드가 없는 인터페이스/Companion/$$serializer 는 카운터가 비어 있다
                classes[cl.get("name")] = cc
    return totals, classes, len(root.findall(".//class"))


def read_tests(path):
    if not os.path.isdir(path):
        return None
    agg = {"tests": 0, "failures": 0, "errors": 0, "skipped": 0}
    try:
        for p in glob.glob(os.path.join(path, "*.xml")):
            r = ET.parse(p).getroot()
            for k in agg:
                agg[k] += int(r.get(k, 0))
    except (OSError, ET.ParseError) as e:
        print(f"[경고] 테스트 결과를 읽지 못했다: {path} ({e})", file=sys.stderr)
        return None
    return agg


def read_exclusions(path):
    if not os.path.exists(path):
        return None
    try:
        text = open(path).read()
    except OSError as e:
        print(f"[경고] 빌드 스크립트를 읽지 못했다: {path} ({e})", file=sys.stderr)
        return None
    m = re.search(r"val coverageExclusions = listOf\((.*?)\n\)", text, re.S)
    if not m:
        return None
    return [l.strip().strip(",").strip('"')
            for l in m.group(1).strip().splitlines() if l.strip()]


def pct(pair):
    cov, tot = pair
    return 100.0 * cov / tot if tot else 0.0


def fmt(pair):
    return f"{pair[0]}/{pair[1]} = {pct(pair):6.2f}%"


def do_generate(args):
    totals, classes, elems = read_xml(args.xml)
    data = {
        "_comment": "커버리지 베이스라인 (F-4). 배치 종료 시 coverage-reporter가 갱신한다. "
                    "각 단계는 이 값을 재산출하지 말고 `coverage-baseline.py check`로 대조하라.",
        "batch": args.batch,
        "measured_at": args.measured_at,
        "git_commit": args.commit,
        "command": "JAVA_HOME=<jbr-17> ./gradlew clean jvmCoverageReport --rerun-tasks",
        "totals": {k: totals[k] for k in METRICS if k in totals},
        "tests": read_tests(args.tests),
        "class_elements": elems,
        "classes_with_counters": len(classes),
        "coverage_exclusions": read_exclusions(args.gradle),
        "classes": dict(sorted(classes.items())),
    }
    with open(args.out, "w") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write("\n")
    print(f"[생성] {args.out}")
    for k in METRICS:
        if k in totals:
            print(f"  {k:12s} {fmt(totals[k])}")
    if data["tests"]:
        t = data["tests"]
        print(f"  테스트         {t['tests']} / 실패 {t['failures']} / skip {t['skipped']}")
    return 0


def do_check(args):
    if not os.path.exists(args.baseline):
        print(f"[오류] 베이스라인 파일이 없다: {args.baseline}\n"
              f"       최초 1회는 `generate`로 만들어라.", file=sys.stderr)
        sys.exit(2)
    base = json.load(open(args.baseline))
    totals, classes, elems = read_xml(args.xml)
    tests = read_tests(args.tests)
    excl = read_exclusions(args.gradle)

    fails = []
    print(f"베이스라인: {base.get('batch')} ({base.get('measured_at')})\n")
    print(f"{'지표':<14}{'베이스라인':>22}{'현재':>22}   판정")
    print("-" * 72)
    for k in METRICS:
        if k not in base["totals"]:
            continue
        b, c = base["totals"][k], totals.get(k)
        if c is None:
            fails.append(f"{k}: 현재 측정치에 없다")
            continue
        # (b) 조항: 비율이 베이스라인보다 낮으면 회귀
        regress = pct(c) < pct(b) - 1e-9
        mark = "회귀" if regress else ("유지" if abs(pct(c) - pct(b)) < 1e-9 else "상승")
        if regress:
            fails.append(f"{k}: {fmt(b)} → {fmt(c)}")
        print(f"{k:<14}{fmt(b):>22}{fmt(c):>22}   {mark}")

    print()
    if tests and base.get("tests"):
        bt, ct = base["tests"], tests
        print(f"테스트         {bt['tests']} → {ct['tests']}  "
              f"(실패 {ct['failures']} / 에러 {ct['errors']} / skip {ct['skipped']})")
        if ct["failures"] or ct["errors"]:
            fails.append(f"테스트 실패 {ct['failures']} / 에러 {ct['errors']}")

    # 제외 범위 확대는 거짓 그린의 대표 경로다 — 집합으로 대조한다
    if excl is not None and base.get("coverage_exclusions") is not None:
        added = [e for e in excl if e not in base["coverage_exclusions"]]
        removed = [e for e in base["coverage_exclusions"] if e not in excl]
        print(f"제외 목록      {len(base['coverage_exclusions'])} → {len(excl)}"
              f"  (추가 {len(added)} / 삭제 {len(removed)})")
        if added:
            fails.append("제외 범위 확대: " + ", ".join(added))
            print("  ! 추가된 패턴:", ", ".join(added))
        if removed:
            print("  - 해제된 패턴:", ", ".join(removed), "(축소 — 문제 아님)")

    # 불변식: 클래스별 covered 하락. 백분율보다 우선한다 —
    # 분모가 같이 줄면 백분율은 그대로인데 실제로 검증이 사라진 경우를 백분율은 못 잡는다.
    dropped = []
    for name, bc in base.get("classes", {}).items():
        cc = classes.get(name)
        if not cc:
            continue
        for k in METRICS:
            if k in bc and k in cc and cc[k][0] < bc[k][0]:
                dropped.append(f"{name}.{k}: covered {bc[k][0]} → {cc[k][0]}")
    if dropped:
        print(f"\n! 클래스별 covered 하락 {len(dropped)}건 (불변식 위반 — 백분율보다 우선):")
        for d in dropped:
            print("   ", d)
        fails.extend(dropped)

    # (a) 조항 보조: 베이스라인 분모에 있던 클래스가 사라졌는지
    vanished = [c for c in base.get("classes", {}) if c not in classes]
    if vanished:
        print(f"\n! 베이스라인 분모에 있던 클래스 {len(vanished)}개가 사라졌다:")
        for c in vanished:
            print("   ", c)
        fails.append(f"분모에서 사라진 클래스 {len(vanished)}개")

    new = [c for c in classes if c not in base.get("classes", {})]
    if new:
        print(f"\n신규 분모 진입 {len(new)}개 (변경 클래스는 70% 조항 대상):")
        for c in new:
            ln = classes[c].get("LINE")
            gate = "" if ln is None else ("  ✅ ≥70%" if pct(ln) >= 70 else "  ❌ <70%")
            print(f"    {c:<52}{'' if ln is None else fmt(ln)}{gate}")
            if ln is not None and pct(ln) < 70:
                fails.append(f"{c} 라인 커버리지 {pct(ln):.2f}% < 70%")

    print()
    if fails:
        print("판정: FAIL")
        for f in fails:
            print("  -", f)
        return 1
    print("판정: PASS  (6개 지표 하락 0건 · 제외 범위 확대 0건)")
    return 0


def main():
    ap = argparse.ArgumentParser(description="커버리지 베이스라인 생성·대조 (F-4)")
    sub = ap.add_subparsers(dest="cmd", required=True)

    for name in ("generate", "check"):
        p = sub.add_parser(name)
        p.add_argument("--xml", default=DEFAULT_XML)
        p.add_argument("--tests", default=DEFAULT_TESTS)
        p.add_argument("--gradle", default=DEFAULT_GRADLE)
        if name == "generate":
            p.add_argument("--out", default=DEFAULT_BASELINE)
            p.add_argument("--batch", default="")
            p.add_argument("--measured-at", dest="measured_at", default="")
            p.add_argument("--commit", default="")
        else:
            p.add_argument("--baseline", default=DEFAULT_BASELINE)

    args = ap.parse_args()
    return do_generate(args) if args.cmd == "generate" else do_check(args)


if __name__ == "__main__":
    sys.exit(main())
