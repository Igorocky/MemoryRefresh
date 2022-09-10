"use strict";

const BulkEditTranslateCardsCmp = ({allTags, usedTags, onApplied, onCancelled}) => {

    const [paused, setPaused] = useState(null)
    const [tagsToAdd, setTagsToAdd] = useState(null)
    const [tagsToRemove, setTagsToRemove] = useState(null)
    const [deleteCards, setDeleteCards] = useState(false)

    function pausedToStr(paused) {
        return paused?'Paused':'Active'
    }

    function renderDeleteCardsControls() {
        return RE.FormGroup({style:{}},
            RE.FormControlLabel({
                control: RE.Checkbox({
                    checked: deleteCards,
                    onChange: event => setDeleteCards(event.target.checked)
                }),
                label: 'Delete selected cards'
            })
        )
    }

    function renderActivenessControls() {
        return RE.Container.row.left.center({},{},
            RE.FormGroup({style:{}},
                RE.FormControlLabel({
                    control: RE.Checkbox({
                        checked: hasValue(paused),
                        onChange: event => {
                            if (event.target.checked) {
                                setPaused(false)
                            } else {
                                setPaused(null)
                            }
                        }
                    }),
                })
            ),
            RE.FormControl({variant:"outlined"},
                RE.InputLabel({id:'paused-select'}, 'Active or paused'),
                RE.Select(
                    {
                        value:paused??false,
                        variant: 'outlined',
                        label: 'Active or paused',
                        labelId: 'paused-select',
                        onChange: event => setPaused(event.target.value),
                        disabled: hasNoValue(paused)
                    },
                    [false, true].map((pausedBool, idx) =>
                        RE.MenuItem({key:idx, value:pausedBool}, pausedToStr(pausedBool))
                    )
                )
            )
        )
    }

    function renderTagsControls({allTags, label, tags, setTags}) {
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
        return renderTagsControls({
            label: 'Add tags', allTags, tags: tagsToAdd, setTags: arg => setTagsToAdd(arg)
        })
    }

    function renderTagsToRemoveControls() {
        return renderTagsControls({
            label: 'Remove tags', allTags: usedTags, tags: tagsToRemove, setTags: arg => setTagsToRemove(arg)
        })
    }

    function prepareResult() {
        if (deleteCards) {
            return {['delete']:true}
        } else {
            return {
                paused,
                addTags: tagsToAdd?.map(t=>t.id),
                removeTags: tagsToRemove?.map(t=>t.id),
            }
        }
    }

    function renderButtons() {
        return RE.Container.row.right.center({style:{marginTop:'15px'}},{},
            RE.Button({onClick: () => onApplied(prepareResult())}, 'apply'),
            RE.Button({onClick: () => onCancelled()}, 'cancel'),
        )
    }

    return RE.Container.col.top.left({},{},
        RE.If(!deleteCards, () => renderActivenessControls()),
        RE.If(!deleteCards, () => renderTagsToAddControls()),
        RE.If(!deleteCards, () => renderTagsToRemoveControls()),
        renderDeleteCardsControls(),
        renderButtons()
    )
}
