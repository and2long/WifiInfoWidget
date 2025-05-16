#!/bin/bash
# 设置错误时退出
set -e

VERSION=$1
CHANGELOG_FILE="CHANGELOG.md"

# 提取版本内容
CHANGELOG=$(awk -v version="$VERSION" '
    BEGIN { found = 0; content = "" }
    /^## \[/ {
        if (found) exit
        if (index($0, "[" version "]") > 0) {
            found = 1
            next
        }
    }
    found {
        if (/^## \[/) exit
        content = content $0 "\n"
    }
    END {
        if (!found) {
            print "错误: 未找到版本 " version " 的更新内容" > "/dev/stderr"
            exit 1
        }
        print content
    }
' "$CHANGELOG_FILE")

echo "$CHANGELOG"