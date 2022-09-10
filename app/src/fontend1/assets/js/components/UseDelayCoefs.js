"use strict";

function useDelayCoefs({showMessageWithProgress, onError}) {
    const [coefs, setCoefs] = useState(null)
    const [errorLoadingCoefs, setErrorLoadingCoefs] = useState(null)

    useEffect(async () => {
        const res = await be.readDelayCoefs()
        if (res.err) {
            setErrorLoadingCoefs(res.err)
            onError(res.err)
        } else {
            setCoefs(res.data)
        }
    }, [])

    async function updateCoefs(newCoefs) {
        const closeProgressIndicator = showMessageWithProgress({text: 'Saving delay coefficients...'})
        const res = await be.updateDelayCoefs({newCoefs})
        closeProgressIndicator()
        if (res.err) {
            onError(res.err)
        } else {
            setCoefs(res.data)
        }
    }

    return {coefs, errorLoadingCoefs, updateCoefs}
}