package com.adobe.acs.commons.one2one.model;

import com.adobe.acs.commons.version.Evolution;
import com.adobe.acs.commons.version.EvolutionAnalyser;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.models.annotations.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

@Model(adaptables = SlingHttpServletRequest.class)
public class One2OneCompareModel {

    private static final Logger log = LoggerFactory.getLogger(One2OneCompareModel.class);

    @Inject
    private ResourceResolver resolver;

    @Inject
    private EvolutionAnalyser analyser;

    private final String path;
    private final Optional<String> pathB;
    private final Optional<String> a;
    private final Optional<String> b;

    private FluentIterable<Evolution> evolutions;
    private Optional<FluentIterable<Evolution>> evolutionsB;

    public One2OneCompareModel(SlingHttpServletRequest request) {
        this.path = request.getParameter("path");
        this.pathB = Optional.fromNullable(request.getParameter("pathB"));
        this.a = Optional.fromNullable(request.getParameter("a"));
        this.b = Optional.fromNullable(request.getParameter("b"));
    }

    @PostConstruct
    protected void activate() {
        this.evolutions = FluentIterable.from(evolutions(path));
        this.evolutionsB = pathB.isPresent() ? Optional.of(FluentIterable.from(evolutions(pathB.get()))) : Optional.<FluentIterable<Evolution>>absent();
    }

    public String getResourcePath() {
        return path;
    }

    public String getResourceB() {
        return pathB.or("");
    }

    public List<VersionSelection> getNames() {
        return evolutions.transform(TO_VERSION_SELECTION).toList();
    }

    public List<VersionSelection> getNamesB() {
        return evolutionsB.or(evolutions).transform(TO_VERSION_SELECTION).toList();
    }

    public List<Evolution> getEvolutions() {
        return Lists.newArrayList(getEvolutionA(), getEvolutionB());
    }

    public Evolution getEvolutionA() {
        return version(evolutions, getA()).orNull();
    }

    public Evolution getEvolutionB() {
        return version(evolutionsB.or(evolutions), getB()).orNull();
    }

    public String getA() {
        return a.or(evolutions.transform(TO_NAME).first().or(""));
    }

    public String getB() {
        return b.or(evolutionsB.or(evolutions).transform(TO_NAME).last().or(""));
    }


    private Optional<Evolution> version(FluentIterable<Evolution> evolutions, final String name) {
        return evolutions.firstMatch(new Predicate<Evolution>() {
            @Override
            public boolean apply(@Nullable Evolution evolution) {
                return evolution.getVersionName().equalsIgnoreCase(name);
            }
        });
    }

    private List<Evolution> evolutions(String path) {
        if (StringUtils.isNotEmpty(path)) {
            Resource resource = resolver.resolve(path);
            if (resource != null && !ResourceUtil.isNonExistingResource(resource)) {
                return analyser.getEvolutionContext(resource).getEvolutionItems();
            }
            log.warn("Could not resolve resource at path={}", path);
        }
        log.warn("No path provided");
        return Collections.emptyList();
    }

    private static final Function<Evolution, String> TO_NAME = new Function<Evolution, String>() {
        @Nullable
        @Override
        public String apply(@Nullable Evolution evolution) {
            return evolution.getVersionName();
        }
    };

    private static final Function<Evolution, VersionSelection> TO_VERSION_SELECTION = new Function<Evolution, VersionSelection>() {
        @Nullable
        @Override
        public VersionSelection apply(@Nullable Evolution evolution) {
            return new VersionSelection(evolution.getVersionName(), evolution.getVersionDate());
        }
    };

}
