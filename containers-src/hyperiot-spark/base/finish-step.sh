#!/bin/bash

#
# Copyright 2019-2023 ACSoftware
#
# Licensed under the Apache License, Version 2.0 (the "License")
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
#

if [ $ENABLE_INIT_DAEMON = "true" ]
   then
       echo "Finish step ${INIT_DAEMON_STEP} in pipeline"
       while true; do
	   sleep 5
	   echo -n '.'
	   string=$(curl -sL -w "%{http_code}" -X PUT $INIT_DAEMON_BASE_URI/finish?step=$INIT_DAEMON_STEP -o /dev/null)
	   [ "$string" = "204" ] && break
       done
       echo "Notified finish of step ${INIT_DAEMON_STEP}"
fi



