#!/bin/bash

# Copyright 2016-2021 Guangzhou Ruishan Information Technology Co. Ltd

set -eo pipefail

#===========================================================================================
# 设置默认值
#===========================================================================================
JVM_XMS=${JVM_XMS:-"512m"}
JVM_XMX=${JVM_XMX:-"1g"}
BASE_DIR=${BASE_DIR:-"/opt/app"}
CONFIG_DIR=${CONFIG_DIR:-"/opt/config/"}

#===========================================================================================
# JVM 配置，只控制Xms和Xmx等项，其他调优配置需要往后再使用
#===========================================================================================
JAVA_OPT="${JAVA_OPT} -server -Xms${JVM_XMS} -Xmx${JVM_XMX}"

#===========================================================================================
# 应用系统相关配置
#===========================================================================================
JAVA_OPT="${JAVA_OPT} -jar ${BASE_DIR}/app.jar"
JAVA_OPT="${JAVA_OPT} ${JAVA_OPT_EXT}"
JAVA_OPT="${JAVA_OPT} --spring.config.additional-location=${CONFIG_DIR}"

exec java ${JAVA_OPT}