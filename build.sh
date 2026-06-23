#!/bin/bash
# سكريبت بناء Professor Client Mod
# تشغيل: chmod +x build.sh && ./build.sh

echo "=== Professor Client Mod Builder ==="

# التحقق من Java 21
if ! java -version 2>&1 | grep -q "21\|22\|23\|24"; then
  echo "⚠  تحتاج Java 21 أو أحدث. حمّله من: https://adoptium.net"
  exit 1
fi

# تنزيل Gradle إذا لم يكن موجوداً
if [ ! -f "./gradlew" ]; then
  GRADLE_VERSION="8.8"
  echo "⬇  تنزيل Gradle $GRADLE_VERSION..."
  curl -L "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -o /tmp/gradle.zip
  unzip -q /tmp/gradle.zip -d /tmp/gradle_dist
  GRADLE_BIN=$(ls -d /tmp/gradle_dist/gradle-*/bin/gradle)
  $GRADLE_BIN wrapper --gradle-version=$GRADLE_VERSION
  chmod +x gradlew
fi

echo "🔨 جاري البناء..."
./gradlew build

if [ $? -eq 0 ]; then
  echo ""
  echo "✅ تم البناء بنجاح!"
  echo "📦 الـ JAR موجود في: build/libs/"
  ls build/libs/*.jar 2>/dev/null
else
  echo "❌ فشل البناء"
fi
