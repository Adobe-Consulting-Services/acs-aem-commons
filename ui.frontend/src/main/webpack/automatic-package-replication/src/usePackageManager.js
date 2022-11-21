import { XMLParser } from 'fast-xml-parser';
import { useState, useEffect } from 'react';
import fetch from 'node-fetch';

export function usePackageManager(cmd, postProcessFn) {
    let [data, setData] = useState();
    let [errors, setErrors] = useState();

    useEffect(() => {
        const parser = new XMLParser();

        async function fetchData() {
            try {
                const response = await fetch('/crx/packmgr/service.jsp?' + new URLSearchParams({cmd: cmd}));
                const xml = await response.text();
                const json = parser.parse(xml);
                const packages = json?.crx?.response?.data?.packages?.package || [];

                let result = packages.filter(p =>                   
                   !['adobe/cq', 
                    'adobe/cq60',
                    'adobe/cq/product', 
                    'adobe/consulting', 
                    'adobe/granite',
                    'com.adobe.aem.graphiql',
                    'com.adobe.cq.inbox',
                    'com.adobe.cq',
                    'day/cq63/product',
                    'day/cq60/fd',
                    'com.adobe.reef',
                    'day/cq60/product',
                    'tmp/repo',
                    'com/adobe/aem/tools',
                    'Adobe'].includes(p.group)
                );

                setData(result);
            } catch(error) {
                setErrors(error);
            };
        }
        fetchData();
    }, [cmd, postProcessFn]);

    return { data, errors }
}
