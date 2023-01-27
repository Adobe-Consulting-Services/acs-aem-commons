import { useState, useEffect } from 'react';
import fetch from 'node-fetch';

export function useAem(url) {
    let [data, setData] = useState();
    let [errors, setErrors] = useState();

    useEffect(() => {
    
        async function fetchData() {
            try {
                const response = await fetch('/etc/acs-commons/dispatcher-flush/test/_jcr_content/configuration.json?' + new URLSearchParams({_nocache: new Date().getTime()}));
                const json = await response.json();
                setData(json);
            } catch(error) {
                setErrors(error);
            };
        }
        fetchData();
    }, [url]);

    return { data, errors }
}
