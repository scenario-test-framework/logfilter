#!/bin/bash
# --------------------------------------------------------------------------------
# production build
# --------------------------------------------------------------------------------
# --------------------------------------------------
# 設定
# --------------------------------------------------
readonly NAME_PROJ="logfilter"
readonly DIR_ARCHIVE="./docs/archive"
readonly PROFILE="production"

readonly SONAR_URL="https://sonarcloud.io"
readonly SONAR_ORGANIZATION="suwa-sh-github"
readonly SONAR_EXCLUDES="src/test/**,**/classification/**,**/dto/**,**/exception/**,**/*Const.java"


# --------------------------------------------------
# build
# --------------------------------------------------
echo --------------------------------------------------
echo  配布ディレクトリ初期化
echo --------------------------------------------------
cd $(cd $(dirname $0); cd ..; pwd;)

if [ -d ${DIR_ARCHIVE} ]; then
    rm -fr ${DIR_ARCHIVE}
fi
mkdir -p ${DIR_ARCHIVE}


echo
echo --------------------------------------------------
echo  build
echo --------------------------------------------------
if [[ "${SONAR_TOKEN}x" = "x" ]]; then
  echo "SONAR_TOKEN が定義されていません。sonar解析をスキップします。"
  readonly CMD_BUILD="mvn clean package site:site -P ${PROFILE} -DPID=$$"
  echo ${CMD_BUILD}
  ${CMD_BUILD}

else
  readonly CMD_BUILD="mvn clean org.jacoco:jacoco-maven-plugin:prepare-agent package site:site sonar:sonar -P ${PROFILE} -DPID=$$"
  echo ${CMD_BUILD}
  ${CMD_BUILD}                                                                                     \
    -Dsonar.host.url=${SONAR_URL}                                                                  \
    -Dsonar.organization=${SONAR_ORGANIZATION}                                                     \
    -Dsonar.login=${SONAR_TOKEN}                                                                   \
    -Dsonar.exclusions="${SONAR_EXCLUDES}" 2>&1 | tee -a ${PATH_LOG} 2>&1
fi

ret_code=$?
if [ ${ret_code} -ne 0 ]; then
    echo "maven buildでエラーが発生しました。" >&2
    exit 1
fi

echo
echo --------------------------------------------------
echo 配布用ファイルの収集
echo --------------------------------------------------
mv target/${NAME_PROJ}*.tar.gz ${DIR_ARCHIVE}

# 結果表示
ls -l "${DIR_ARCHIVE}"

exit 0
