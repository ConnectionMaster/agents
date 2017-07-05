/**
 * Copyright (C) 2012 White Source Ltd.
 *
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
package org.whitesource.agent.api;

/**
 * A class to hold constant values used in WhiteSource agents.
 * 
 * @author tom.shapira
 */
public final class APIConstants {
	
	public static final String API_VERSION = "2.1.0";
	
	/* --- Parameters --- */
	
	public static final String PARAM_TOKEN = "token";
	public static final String PARAM_PRODUCT = "product";
	public static final String PARAM_PRODUCT_VERSION = "productVersion";
	public static final String PARAM_DIFF = "diff";
	public static final String PARAM_DEPENDENCIES = "dependencies";
	public static final String PARAM_AGENT = "agent";
	public static final String PARAM_AGENT_VERSION = "agentVersion";
	public static final String PARAM_PLUGIN_VERSION = "pluginVersion";
	public static final String PARAM_REQUEST_TYPE = "type";
    public static final String PARAM_TIME_STAMP = "timeStamp";
    public static final String PARAM_REQUESTER_EMAIL = "requesterEmail";
	public static final String PARAM_FORCE_CHECK_ALL_DEPENDENCIES = "forceCheckAllDependencies";

	/* --- Messages --- */
	
	public static final String TOKEN_INVALID = "Invalid token";
	public static final String TIME_STAMP_INVALID = "Invalid request time";
	public static final String DIFF_INVALID = "Invalid diff";
	public static final String UPDATE_SUCCESS = "update success";
	public static final String JSON_ERROR = "Problem parsing json";
	
	/* --- Miscellaneous --- */
	
	public static final int HASH_CODE_SEED = 133;
	public static final int HASH_CODE_FACTOR = 23;
	
	/* --- Constructors --- */
	
	/**
	 * Private default constructor
	 */
	private APIConstants() {
		// avoid instantiation
	}

}