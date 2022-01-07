"use strict";

function useTranslateCardHistory({cardId, tabIndex}) {
    const {renderMessagePopup, showError} = useMessagePopup()

    const [validationHistory, setValidationHistory] = useState(null)
    const [validationHistoryRequested, setValidationHistoryRequested] = useState(false)
    const [errorLoadingHistory, setErrorLoadingHistory] = useState(null)

    async function loadHistory() {
        setValidationHistoryRequested(true)
        const res = await be.readTranslateCardHistory({cardId})
        if (res.err) {
            setErrorLoadingHistory(res.err)
            showError(res.err)
        } else {
            setValidationHistory(res.data)
        }
    }

    function timestampToStr(timestamp) {
        const timeStr = new Date(timestamp).toString()
        const idx = timeStr.indexOf(' (')
        return timeStr.substring(0,idx)
    }

    function renderHistoryTable() {
        const rows = []
        validationHistory.dataHistory.forEach(dataRec => {
            const validationRecBackgroundColor = '#f1f1f1'
            dataRec.validationHistory.forEach((validationRec,idx) => {
                const bgColor = idx % 2 === 1 ? validationRecBackgroundColor : undefined
                rows.push(RE.tr({key:validationRec.recId+'-v1', style:{backgroundColor:bgColor}},
                    RE.td({rowSpan:2},
                        RE.span({style:{fontWeight:'bold',color:validationRec.isCorrect ? 'green' : 'red'}},validationRec.isCorrect ? '\u{02713}' : '\u{02717}'),
                    ),
                    RE.td({style:{paddingLeft:'5px', paddingRight:'5px'}}, validationRec.actualDelay,),
                    RE.td({}, timestampToStr(validationRec.timestamp))
                ))
                rows.push(RE.tr({key:validationRec.recId+'-v2', style:{backgroundColor:bgColor}},
                    RE.td({colSpan:2}, validationRec.translation),
                ))
            })
            const dataRecBackgroundColor = '#dedede'
            rows.push(RE.tr({key:dataRec.verId+'-1', style:{backgroundColor: dataRecBackgroundColor}},
                RE.td({colSpan:3},timestampToStr(dataRec.timestamp))
            ))
            rows.push(RE.tr({key:dataRec.verId+'-2', style:{backgroundColor: dataRecBackgroundColor}},
                RE.td({colSpan:3},dataRec.textToTranslate)
            ))
            rows.push(RE.tr({key:dataRec.verId+'-3', style:{backgroundColor: dataRecBackgroundColor}},
                RE.td({colSpan:3},dataRec.translation)
            ))

        })

        return RE.table({className:'table-with-collapsed-borders gray'}, RE.tbody({},
            rows
        ))
    }

    function renderValidationHistory() {
        if (hasValue(validationHistory)) {
            return RE.Container.col.top.left({},{},
                RE.span({style:{fontWeight: 'bold'}}, 'History:'),
                renderHistoryTable()
            )
        } else if (hasValue(errorLoadingHistory)) {
            return RE.Fragment({},
                `An error occurred during loading of validation history: [${errorLoadingHistory.code}] - ${errorLoadingHistory.msg}`,
            )
        } else {
            return buttonWithCircularProgress({
                onClick: loadHistory,
                text: 'Load history',
                showProgress: validationHistoryRequested,
                buttonAttrs: {tabIndex}
            })
        }
    }

    return {
        renderValidationHistory: () => RE.Fragment({},
            renderValidationHistory(),
            renderMessagePopup()
        )
    }
}