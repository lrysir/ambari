"""
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Ambari Agent

"""
from resource_management import *
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
from ambari_commons import OSConst

@OsFamilyFuncImpl(os_family=OSConst.WINSRV_FAMILY)
def webhcat_service(action='start'):
  import params
  if action == 'start' or action == 'stop':
    Service(params.webhcat_server_win_service_name, action=action)


@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def webhcat_service(action='start', rolling_restart=False):
  import params

  environ = {
    'HADOOP_HOME': params.hadoop_home
  }

  cmd = format('{webhcat_bin_dir}/webhcat_server.sh')

  if action == 'start':
    if rolling_restart and params.version:
      environ['HADOOP_HOME'] = format("/usr/hdp/{version}/hadoop")

    daemon_cmd = format('cd {hcat_pid_dir} ; {cmd} start')
    no_op_test = format('ls {webhcat_pid_file} >/dev/null 2>&1 && ps -p `cat {webhcat_pid_file}` >/dev/null 2>&1')
    Execute(daemon_cmd,
            user=params.webhcat_user,
            not_if=no_op_test,
            environment = environ)
  elif action == 'stop':
    daemon_cmd = format('{cmd} stop')
    Execute(daemon_cmd,
            user = params.webhcat_user,
            environment = environ)
    File(params.webhcat_pid_file,
         action="delete",
    )
