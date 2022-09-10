"use strict";

function useMaxDelay({showMessageWithProgress, onError}) {
    const [maxDelay, setMaxDelay] = useState(null)
    const [errorLoadingMaxDelay, setErrorLoadingMaxDelay] = useState(null)

    useEffect(async () => {
        const res = await be.readMaxDelay()
        if (res.err) {
            setErrorLoadingMaxDelay(res.err)
            onError(res.err)
        } else {
            setMaxDelay(res.data)
        }
    }, [])

    async function updateMaxDelay(newMaxDelay) {
        const closeProgressIndicator = showMessageWithProgress({text: 'Saving max delay...'})
        const res = await be.updateMaxDelay({newMaxDelay})
        closeProgressIndicator()
        if (res.err) {
            onError(res.err)
        } else {
            setMaxDelay(res.data)
        }
    }

    return {maxDelay, errorLoadingMaxDelay, updateMaxDelay}
}