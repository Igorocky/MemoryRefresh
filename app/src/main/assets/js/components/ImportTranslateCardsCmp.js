"use strict";

const ImportTranslateCardsCmp = ({importFileName, onImport, onCancelled}) => {
    const {showError} = useMessagePopup()

    const {allTags, allTagsMap, errorLoadingTags} = useTags()


    const [errorLoadingImportFileInfo, setErrorLoadingImportFileInfo] = useState(null)
    const [numberOfCards, setNumberOfCards] = useState(null)
    const [newTags, setNewTags] = useState(null)
    const [tagsToAdd, setTagsToAdd] = useState(null)

    useEffect(async () => {
        const res = await be.getImportTranslateCardsInfo({fileName: importFileName})
        if (res.err) {
            setErrorLoadingImportFileInfo(res.err)
            showError(res.err)
        } else {
            setNumberOfCards(res.data.numberOfCards)
            setNewTags(res.data.newTags)
        }
    }, [])

    function renderTagsControls({label, tags, setTags}) {
        return RE.Container.row.left.center({},{},
            RE.FormGroup({style:{}},
                RE.FormControlLabel({
                    control: RE.Checkbox({
                        checked: hasValue(tags),
                        onChange: event => {
                            if (event.target.checked) {
                                setTags([])
                            } else {
                                setTags(null)
                            }
                        }
                    }),
                    label
                })
            ),
            RE.If(hasValue(tags), () => RE.Paper({style: {}},re(TagSelector,{
                allTags,
                selectedTags: tags,
                onTagRemoved:tag=> setTags(prev => prev.filter(t => t.id !== tag.id)),
                onTagSelected:tag=> setTags(prev => [...prev, tag]),
                label,
                color:'primary',
            })))
        )
    }

    function renderTagsToAddControls() {
        return renderTagsControls({label: 'Add tags', tags: tagsToAdd, setTags: arg => setTagsToAdd(arg)})
    }

    function renderButtons() {
        return RE.Container.row.right.center({style:{marginTop:'15px'}},{},
            RE.Button({onClick: () => onImport({additionalTags: (tagsToAdd??[]).map(t=>t.id)})}, 'import'),
            RE.Button({onClick: onCancelled}, 'cancel'),
        )
    }

    function renderImportDialog() {
        return RE.Container.col.top.left({},{style:{marginTop: '15px'}},
            RE.span({}, `Number of cards being imported: ${numberOfCards}`),
            RE.If(newTags.length, () => RE.span({}, `New tags: ${newTags.join(', ')}`,)),
            renderTagsToAddControls(),
            renderButtons()
        )
    }

    if (errorLoadingTags) {
        return RE.Fragment({},
            `An error occurred during loading of tags: [${errorLoadingTags.code}] - ${errorLoadingTags.msg}`,
        )
    } else if (errorLoadingImportFileInfo) {
        return RE.Fragment({},
            `An error occurred during loading of import file info: [${errorLoadingImportFileInfo.code}] - ${errorLoadingImportFileInfo.msg}`,
        )
    } else if (hasNoValue(allTags)) {
        return 'Loading tags...'
    } else if (hasNoValue(numberOfCards) || hasNoValue(newTags)) {
        return 'Loading cards info...'
    } else {
        return renderImportDialog()
    }
}
