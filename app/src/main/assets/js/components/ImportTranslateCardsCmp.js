"use strict";

const ImportTranslateCardsCmp = ({fileUri, numberOfCards, newTags, onImport, onCancelled}) => {
    const {allTags, allTagsMap, errorLoadingTags} = useTags()

    const [paused, setPaused] = useState(true)
    const [tagsToAdd, setTagsToAdd] = useState(null)

    function pausedToStr(paused) {
        return paused?'Paused':'Active'
    }

    function renderActivenessControls() {
        return RE.Container.row.left.center({},{},
            'Save cards as',
            RE.FormControl({variant:"outlined", style:{marginLeft:'15px'}},
                RE.Select(
                    {
                        value:paused??false,
                        variant: 'outlined',
                        labelId: 'paused-select',
                        onChange: event => setPaused(event.target.value),
                    },
                    [false, true].map((pausedBool, idx) =>
                        RE.MenuItem({key:idx, value:pausedBool}, pausedToStr(pausedBool))
                    )
                )
            )
        )
    }

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
            RE.Button({
                onClick: () => onImport({fileUri, paused, additionalTags: (tagsToAdd??[]).map(t=>t.id)})
            }, 'import'),
            RE.Button({onClick: onCancelled}, 'cancel'),
        )
    }

    function renderImportDialog() {
        return RE.Container.col.top.left({},{style:{marginTop: '15px'}},
            RE.span({}, `Number of cards being imported: ${numberOfCards}`),
            RE.If(newTags.length, () => RE.span({}, `New tags: ${newTags.join(', ')}`,)),
            renderActivenessControls(),
            renderTagsToAddControls(),
            renderButtons()
        )
    }

    if (errorLoadingTags) {
        return RE.Fragment({},
            `An error occurred during loading of tags: [${errorLoadingTags.code}] - ${errorLoadingTags.msg}`,
        )
    } else if (hasNoValue(allTags)) {
        return 'Loading tags...'
    } else {
        return renderImportDialog()
    }
}
