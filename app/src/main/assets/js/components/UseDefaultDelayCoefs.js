"use strict";

function useDefaultDelayCoefs({showMessageWithProgress, onError}) {
    const [coefs, setCoefs] = useState(null)
    const [errorLoadingCoefs, setErrorLoadingCoefs] = useState(null)

    useEffect(async () => {
        const res = await be.readDefaultDelayCoefs()
        if (res.err) {
            setErrorLoadingCoefs(res.err)
            onError(res.err)
        } else {
            setCoefs(res.data)
        }
    }, [])

    async function updateCoefs(newDefCoefs) {
        const closeProgressIndicator = showMessageWithProgress({text: 'Saving default delay coefficients...'})
        const res = await be.updateDefaultDelayCoefs({newDefCoefs})
        closeProgressIndicator()
        if (res.err) {
            onError(res.err)
        } else {
            setCoefs(res.data)
        }
    }

    return {defCoefs:coefs, errorLoadingDefCoefs:errorLoadingCoefs, updateDefCoefs:updateCoefs}
}