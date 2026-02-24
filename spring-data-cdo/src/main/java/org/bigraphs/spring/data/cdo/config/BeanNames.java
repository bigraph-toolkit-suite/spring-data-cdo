/*
 * Copyright 2011-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.bigraphs.spring.data.cdo.config;

/**
 * Constants to declare bean names used by the namespace configuration.
 *
 * @author Jon Brisbin
 * @author Oliver Gierke
 * @author Martin Baumgartner
 * @author Christoph Strobl
 */
public abstract class BeanNames {

    public static final String MAPPING_CONTEXT_BEAN_NAME = "cdoMappingContext";
    public static final String CDO_BEAN_NAME = "cdoClient";
    public static final String DB_FACTORY_BEAN_NAME = "cdoDbFactory";
    public static final String CDO_TEMPLATE_BEAN_NAME = "cdoTemplate";
}
