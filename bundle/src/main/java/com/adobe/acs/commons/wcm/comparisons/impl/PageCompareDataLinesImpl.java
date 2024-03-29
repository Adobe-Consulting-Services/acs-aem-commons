/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.adobe.acs.commons.wcm.comparisons.impl;

import com.adobe.acs.commons.wcm.comparisons.PageCompareDataLine;
import com.adobe.acs.commons.wcm.comparisons.impl.lines.LinesGenerator;
import com.adobe.acs.commons.wcm.comparisons.lines.Line;
import com.adobe.acs.commons.wcm.comparisons.PageCompareDataLines;
import com.google.common.base.Function;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.List;

@Component(metatype = false)
@Service
public class PageCompareDataLinesImpl implements PageCompareDataLines {

    @Override
    public List<Line<PageCompareDataLine>> generate(Iterable<PageCompareDataLine> left, Iterable<PageCompareDataLine> right) {
        LinesGenerator<PageCompareDataLine> generator = new LinesGenerator<PageCompareDataLine>(new Function<PageCompareDataLine, Serializable>() {
            @Nullable
            @Override
            public Serializable apply(@Nullable PageCompareDataLine input) {
                return input.getUniqueName();
            }
        });
        return generator.generate(left, right);
    }
}
