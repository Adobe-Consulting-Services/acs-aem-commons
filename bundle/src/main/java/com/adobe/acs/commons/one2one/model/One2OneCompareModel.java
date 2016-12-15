package com.adobe.acs.commons.one2one.model;

import com.adobe.acs.commons.version.Evolution;
import com.adobe.acs.commons.version.EvolutionAnalyser;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
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

    private final String pathA;
    private final Optional<String> versionA;
    private final Optional<String> pathB;
    private final Optional<String> versionB;

    private FluentIterable<Evolution> evolutionsA;
    private Optional<FluentIterable<Evolution>> evolutionsB;

    public One2OneCompareModel(SlingHttpServletRequest request) {
        this.pathA = request.getParameter("path");
        this.pathB = Optional.fromNullable(request.getParameter("pathB"));
        this.versionA = Optional.fromNullable(request.getParameter("a"));
        this.versionB = Optional.fromNullable(request.getParameter("b"));
    }

    @PostConstruct
    protected void activate() {
        this.evolutionsA = FluentIterable.from(evolutions(pathA));
        this.evolutionsB = pathB.isPresent() ? Optional.of(FluentIterable.from(evolutions(pathB.get()))) : Optional.<FluentIterable<Evolution>>absent();
    }

    public String getResourcePathA() {
        return pathA;
    }

    public String getResourcePathB() {
        return pathB.or("");
    }

    public List<VersionSelection> getVersionSelectionsA() {
        return evolutionsA.transform(TO_VERSION_SELECTION).toList();
    }

    public List<VersionSelection> getVersionSelectionsB() {
        return evolutionsB.or(evolutionsA).transform(TO_VERSION_SELECTION).toList();
    }

    public Evolution getEvolutionA() {
        return version(evolutionsA, getVersionA()).orNull();
    }

    public Evolution getEvolutionB() {
        return version(evolutionsB.or(evolutionsA), getVersionB()).orNull();
    }

    public String getVersionA() {
        return versionA.or(evolutionsA.transform(TO_NAME).first().or(""));
    }

    public String getVersionB() {
        return versionB.or(evolutionsB.or(evolutionsA).transform(TO_NAME).last().or(""));
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
