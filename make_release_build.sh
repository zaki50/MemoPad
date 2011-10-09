#!/bin/sh
#set -x

# このスクリプトは、ant でリリースビルドを作成し、
# バージョン番号(android:versionNameから取得)の
# ディレクトリに成果物をコピーします。
# バージョン番号のディレクトリが既に存在する場合は
# ビルドを中断しますが、既存の成果物を上書きしたい
# 場合は引き数に -f をつけてください。

force=0
if [ x"$1" = x"-f" ]; then
  force=1
fi

# プロジェクトディレクトリをカレントディレクトにする
projectdir=$(dirname $0)
echo "changing directory to ${projectdir}" >&2
pushd "${projectdir}" > /dev/null

echo "creating destination directory" >&2

# 念のため android:versionName が含まれていることを確認
if ! grep -q android:versionName AndroidManifest.xml; then
  echo "failed to get version name. exit." >&2
  popd > /dev/null
  exit 1
fi

# android:versionName の記述からバージョンを取得する
versionName=$(grep android:versionName AndroidManifest.xml | sed -e 's/.*android:versionName="\([0-9][0-9.]*[0-9]\)".*/\1/')
if [ x"$versionName" = x ]; then
  echo "failed to get version name. exit." >&2
  popd > /dev/null
  exit 1
fi

#ビルドした成果物の格納先
dest="released_binaries/${versionName}"
if [ -e "${dest}" ]; then
  if [ $force -eq 1 ]; then
    rm -rf "${dest}/*"
  else
    echo "destination directory already exists. Is versionName correct?" >&2
    echo "add -f to arguments if you intended to overwrite." >&2
    popd > /dev/null
    exit 1
  fi
fi
mkdir -p "${dest}"
echo "destinationdirectory created: ${dest}" >&2

# ant clean の実行
rm -f ./ant_clean.log ./ant_release.log
echo "invoking 'ant clean'. output is written to ant_clean.log" >&2
if ! ant clean > ant_clean.log; then
  cat ./ant_clean.log
  echo "" >&2
  echo "failed to clean project. exit." >&2
  popd > /dev/null
  exit 1
fi

# ant release の実行
echo "updating build.xml" >&2
rm -f build.xml
android update project -p .
echo "invoking 'ant release'. output is written to ant_release.log" >&2
if ! ant release | tee ./ant_release.log | grep "Please enter"; then
  cat ./ant_release.log
  echo "" >&2
  echo "BUILD FAILED" >&2
  popd > /dev/null
  exit 1
fi

# 署名失敗の場合はどうも ant 自体はエラー扱いになっていないので独自にチェック
if [ ! -e bin/*-release.apk ]; then
  cat ./ant_release.log
  echo "" >&2
  echo "BUILD FAILED" >&2
  popd > /dev/null
  exit 1
fi

# 成果物コピー
echo "copying artifacts to ${dest}" >&2
cp -a bin/*-release.apk "${dest}/"
cp -a bin/proguard "${dest}/"
mv ant_release.log "${dest}/"
rm -f ant_clean.log

# おしまい
echo "" >&2
echo "BUILD SUCCESSFUL" >&2
popd > /dev/null
exit 0
