#!/bin/sh
set -x

# findbugs 関連の記述をコードから取り除く
find src/ -name \*.java -exec sed -e 's/^import edu\.umd\.cs\.findbugs\.annotations\.[^;]*;$//' -e 's/@DefaultAnnotation(NonNull.class)//' -e 's/@CheckForNull//' -i "" {} \;

# findbugs 関連の jar を classpath から取り除く
sed -e 's/<classpathentry kind="lib" path="libs\/findbugs-[0-9.]*\.jar"\/>//' -e 's/<classpathentry kind="lib" path="libs\/jsr305-[0-9.]*\.jar"\/>//' -i "" .classpath

# ant が参照してしまわないように使ってないライブラリを削除
rm -f libs/findbugs-*.jar
rm -f libs/jsr305-*.jar

# debuggable を false にする
sed -e 's/android:debuggable="true"/android:debuggable="false"/' -i "" AndroidManifest.xml

