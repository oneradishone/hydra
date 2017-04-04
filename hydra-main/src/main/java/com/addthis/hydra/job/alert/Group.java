/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.addthis.hydra.job.alert;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * User group alert configuration
 */
public class Group {
    public final String name;
    @Nullable public final String email;
    @Nullable public final String pagerEmail;
    @Nullable public final String webhookURL;

    @JsonCreator public Group(@JsonProperty(value = "name") String name,
                              @Nullable @JsonProperty(value = "email") String email,
                              @Nullable @JsonProperty(value = "pagerEmail") String pagerEmail,
                              @Nullable @JsonProperty(value = "webhookURL") String webhookURL) {
        this.name = name;
        this.email = email;
        this.pagerEmail = pagerEmail;
        this.webhookURL = webhookURL;
    }
}
