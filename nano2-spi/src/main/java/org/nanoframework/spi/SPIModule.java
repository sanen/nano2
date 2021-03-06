/*
 * Copyright 2015-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.nanoframework.spi;

import java.util.List;

import org.nanoframework.beans.Globals;
import org.nanoframework.spi.annotation.Order;
import org.nanoframework.spi.def.Module;
import org.nanoframework.spi.support.SPILoader;
import org.nanoframework.spi.support.SPIMapper;
import org.nanoframework.spi.support.SPIProvider;
import org.nanoframework.toolkit.lang.CollectionUtils;

import com.google.inject.Binder;
import com.google.inject.Injector;
import com.google.inject.name.Names;

/**
 * @author yanghe
 * @since 1.4.8
 */
@Order(-9999)
public class SPIModule implements Module {

    @Override
    public void configure(Binder binder) {
        var spiMappers = SPILoader.spis();
        if (!CollectionUtils.isEmpty(spiMappers)) {
            var injector = Globals.get(Injector.class);
            spiMappers.forEach((spiCls, spis) -> bind(binder, spis, injector));
        }
    }

    @SuppressWarnings("unchecked")
    private void bind(Binder binder, List<SPIMapper> spis, Injector injector) {
        spis.forEach(spi -> binder.bind(spi.getSpi()).annotatedWith(Names.named(spi.getName()))
                .toProvider(new SPIProvider(spi)));
    }

    @Override
    public List<Module> load() {
        return null;
    }

    @Override
    public void destroy() {
        SPILoader.clear();
    }

}
