"use strict";

const AVAILABLE_TRANSLATE_CARD_FILTERS = {
    INCLUDE_TAGS:'INCLUDE_TAGS',
    EXCLUDE_TAGS:'EXCLUDE_TAGS',
    CREATED_ON_OR_AFTER:'CREATED_ON_OR_AFTER',
    CREATED_ON_OR_BEFORE:'CREATED_ON_OR_BEFORE',
    NATIVE_TEXT_LENGTH:'NATIVE_TEXT_LENGTH',
    FOREIGN_TEXT_LENGTH:'FOREIGN_TEXT_LENGTH',
}

const TranslateCardFilterCmp = ({onSubmit}) => {
    const {renderMessagePopup, showMessage, confirmAction, showError, showDialog} = useMessagePopup()
    const af = AVAILABLE_TRANSLATE_CARD_FILTERS

    const [allTags, setAllTags] = useState(null)
    const [allTagsMap, setAllTagsMap] = useState(null)
    const [errorLoadingTags, setErrorLoadingTags] = useState(null)

    const [cardToTagsMap, setCardToTagsMap] = useState(null)
    const [errorLoadingCardToTagsMap, setErrorLoadingCardToTagsMap] = useState(null)

    const [filtersSelected, setFiltersSelected] = useState([af.NATIVE_TEXT_LENGTH])
    const [focusedFilter, setFocusedFilter] = useState(filtersSelected[0])

    const [tagsToInclude, setTagsToInclude] = useState([])
    const [tagsToExclude, setTagsToExclude] = useState([])
    const [remainingTags, setRemainingTags] = useState([])

    const [createdOnOrAfter, setCreatedOnOrAfter] = useState(new Date())
    const [createdOnOrBefore, setCreatedOnOrBefore] = useState(new Date())

    const [nativeTextMinLength, setNativeTextMinLength] = useState(null)
    const [nativeTextMaxLength, setNativeTextMaxLength] = useState(null)
    const [foreignTextMinLength, setForeignTextMinLength] = useState(null)
    const [foreignTextMaxLength, setForeignTextMaxLength] = useState(null)

    useEffect(async () => {
        const res = await be.readAllTags()
        if (res.err) {
            setErrorLoadingTags(res.err)
            showError(res.err)
        } else {
            const allTags = res.data
            setAllTags(allTags)
            const allTagsMap = {}
            for (const tag of allTags) {
                allTagsMap[tag.id] = tag
            }
            setAllTagsMap(allTagsMap)

            const res2 = await be.getCardToTagMapping()
            if (res2.err) {
                setErrorLoadingCardToTagsMap(res2.err)
                showError(res2.err)
            } else {
                setCardToTagsMap(res2.data)
            }
        }
    }, [])

    useEffect(async () => {
        if (cardToTagsMap) {
            recalculateRemainingTags()
        }
    }, [cardToTagsMap, tagsToInclude, tagsToExclude])

    function recalculateRemainingTags() {
        const remainingTagIds = []
        for (const cardId in cardToTagsMap) {
            const cardTagIds = cardToTagsMap[cardId]
            let cardPassed = true
            for (const tag of tagsToInclude) {
                if (!cardTagIds.includes(tag.id)) {
                    cardPassed = false
                    break
                }
            }
            if (cardPassed) {
                for (const tag of tagsToExclude) {
                    if (cardTagIds.includes(tag.id)) {
                        cardPassed = false
                        break
                    }
                }
                if (cardPassed) {
                    remainingTagIds.push(...cardTagIds)
                }
            }
        }
        const tagIdsToInclude = tagsToInclude.map(t=>t.id)
        const tagIdsToExclude = tagsToExclude.map(t=>t.id)
        const remainingTags = remainingTagIds
            .distinct()
            .filter(tagId => !tagIdsToInclude.includes(tagId) && !tagIdsToExclude.includes(tagId))
            .map(tagId => allTagsMap[tagId])
        setRemainingTags(remainingTags)
    }

    function addFilter(name) {
        setFiltersSelected(prev => [name, ...prev])
        setFocusedFilter(name)
    }

    function removeFilter(name) {
        setFiltersSelected(prev => prev.filter(n => n !== name))
    }

    function renderTagsToInclude() {
        const filterName = af.INCLUDE_TAGS
        return RE.Container.col.top.left({},{},
            RE.Container.row.left.center({},{},
                iconButton({
                    iconName:'cancel',
                    onClick: () => {
                        removeFilter(filterName)
                        setTagsToInclude([])
                    }}
                ),
                RE.span({style:{paddingRight:'10px'}, onClick: () => setFocusedFilter(null)}, 'Tags to include:')
            ),
            re(TagSelector,{
                allTags: remainingTags,
                selectedTags: tagsToInclude,
                onTagRemoved:tag=>{
                    setTagsToInclude(prev=>prev.filter(t=>t.id!=tag.id))
                },
                onTagSelected:tag=>{
                    setTagsToInclude(prev=>[...prev,tag])
                },
                label: 'Include',
                color:'primary',
                minimized: filterName !== focusedFilter,
            })
        )
    }

    function renderTagsToExclude() {
        const filterName = af.EXCLUDE_TAGS
        return RE.Container.col.top.left({},{},
            RE.Container.row.left.center({},{},
                iconButton({
                    iconName:'cancel',
                    onClick: () => {
                        removeFilter(filterName)
                        setTagsToExclude([])
                    }
                }),
                RE.span({style:{paddingRight:'10px'}, onClick: () => setFocusedFilter(null)}, 'Tags to exclude:')
            ),
            re(TagSelector,{
                allTags: remainingTags,
                selectedTags: tagsToExclude,
                onTagRemoved:tag=>{
                    setTagsToExclude(prev=>prev.filter(t=>t.id!=tag.id))
                },
                onTagSelected:tag=>{
                    setTagsToExclude(prev=>[...prev,tag])
                },
                label: 'Exclude',
                color:'secondary',
                minimized: filterName !== focusedFilter,
            })
        )
    }

    function renderCreatedOnOrAfter() {
        const filterName = af.CREATED_ON_OR_AFTER
        return RE.Container.col.top.left({},{},
            RE.Container.row.left.center({},{},
                iconButton({
                    iconName:'cancel',
                    onClick: () => {
                        removeFilter(filterName)
                        setCreatedOnOrAfter(new Date())
                    }
                }),
                RE.span({style:{paddingRight:'10px'}, onClick: () => setFocusedFilter(null)}, 'Created on or after:')
            ),
            re(DateSelector,{
                selectedDate: createdOnOrAfter,
                onDateSelected: newDate => setCreatedOnOrAfter(newDate),
                minimized: filterName !== focusedFilter,
            })
        )
    }

    function renderCreatedOnOrBefore() {
        const filterName = af.CREATED_ON_OR_BEFORE
        return RE.Container.col.top.left({},{},
            RE.Container.row.left.center({},{},
                iconButton({
                    iconName:'cancel',
                    onClick: () => {
                        removeFilter(filterName)
                        setCreatedOnOrBefore(new Date())
                    }
                }),
                RE.span({style:{paddingRight:'10px'}, onClick: () => setFocusedFilter(null)}, 'Created on or before:')
            ),
            re(DateSelector,{
                selectedDate: createdOnOrBefore,
                onDateSelected: newDate => setCreatedOnOrBefore(newDate),
                minimized: filterName !== focusedFilter,
            })
        )
    }

    function renderNativeTextLength() {
        const filterName = af.NATIVE_TEXT_LENGTH
        return RE.Container.col.top.left({},{},
            RE.Container.row.left.center({},{},
                iconButton({
                    iconName:'cancel',
                    onClick: () => {
                        removeFilter(filterName)
                        setNativeTextMinLength(null)
                        setNativeTextMaxLength(null)
                    }
                }),
                RE.span({style:{paddingRight:'10px'}, onClick: () => setFocusedFilter(null)}, 'Native text length:')
            ),
            re(IntRangeSelector,{
                selectedMin: nativeTextMinLength,
                selectedMax: nativeTextMaxLength,
                onMinSelected: newValue => setNativeTextMinLength(newValue),
                onMaxSelected: newValue => setNativeTextMaxLength(newValue),
                parameterName: '(native text length)',
                minimized: filterName !== focusedFilter,
            })
        )
    }

    function renderForeignTextLength() {
        const filterName = af.FOREIGN_TEXT_LENGTH
        return RE.Container.col.top.left({},{},
            RE.Container.row.left.center({},{},
                iconButton({
                    iconName:'cancel',
                    onClick: () => {
                        removeFilter(filterName)
                        setForeignTextMinLength(null)
                        setForeignTextMaxLength(null)
                    }
                }),
                RE.span({style:{paddingRight:'10px'}, onClick: () => setFocusedFilter(null)}, 'Foreign text length:')
            ),
            re(IntRangeSelector,{
                selectedMin: foreignTextMinLength,
                selectedMax: foreignTextMaxLength,
                onMinSelected: newValue => setForeignTextMinLength(newValue),
                onMaxSelected: newValue => setForeignTextMaxLength(newValue),
                parameterName: '(foreign text length)',
                minimized: filterName !== focusedFilter,
            })
        )
    }

    function renderSelectedFilters() {
        return RE.Container.col.top.left({style:{marginTop:'5px'}},{style:{marginTop:'5px'}},
            filtersSelected.map(filterName => RE.Paper({onClick: () => focusedFilter !== filterName ? setFocusedFilter(filterName) : null},
                (filterName === af.INCLUDE_TAGS) ? renderTagsToInclude()
                : (filterName === af.EXCLUDE_TAGS) ? renderTagsToExclude()
                : (filterName === af.CREATED_ON_OR_AFTER) ? renderCreatedOnOrAfter()
                : (filterName === af.CREATED_ON_OR_BEFORE) ? renderCreatedOnOrBefore()
                : (filterName === af.NATIVE_TEXT_LENGTH) ? renderNativeTextLength()
                : (filterName === af.FOREIGN_TEXT_LENGTH) ? renderForeignTextLength()
                : `unexpected filter - ${filterName}`
            ))
        )
    }

    function renderAvailableFilterListItem({filterName, filterDisplayName, resolve}) {
        return RE.If(!filtersSelected.includes(filterName), () => RE.ListItem(
            {button:true, onClick: () => resolve(filterName)},
            RE.ListItemText({}, filterDisplayName)
        ))
    }

    function renderListOfAvailableFilters(resolve) {
        return RE.List({},
            renderAvailableFilterListItem({filterName: af.INCLUDE_TAGS, filterDisplayName: 'Tags to include', resolve}),
            renderAvailableFilterListItem({filterName: af.EXCLUDE_TAGS, filterDisplayName: 'Tags to exclude', resolve}),
            renderAvailableFilterListItem({filterName: af.CREATED_ON_OR_AFTER, filterDisplayName: 'Created on or after', resolve}),
            renderAvailableFilterListItem({filterName: af.CREATED_ON_OR_BEFORE, filterDisplayName: 'Created on or before', resolve}),
            renderAvailableFilterListItem({filterName: af.NATIVE_TEXT_LENGTH, filterDisplayName: 'Native text length', resolve}),
            renderAvailableFilterListItem({filterName: af.FOREIGN_TEXT_LENGTH, filterDisplayName: 'Foreign text length', resolve}),
        )
    }

    function renderAddFilterButton() {
        return iconButton({
            iconName: 'playlist_add',
            onClick: async () => {
                const selectedFilter = await showDialog({
                    title: 'Select filter:',
                    cancelBtnText: 'cancel',
                    contentRenderer: resolve => {
                        return renderListOfAvailableFilters(resolve)
                    }
                })
                if (hasValue(selectedFilter)) {
                    addFilter(selectedFilter)
                }
            }
        })
    }

    function renderComponentContent() {
        if (errorLoadingTags) {
            return RE.Fragment({},
                `An error occurred during loading of tags: [${errorLoadingTags.code}] - ${errorLoadingTags.msg}`,
            )
        } else if (errorLoadingCardToTagsMap) {
            return RE.Fragment({},
                `An error occurred during loading of card to tags mapping: [${errorLoadingCardToTagsMap.code}] - ${errorLoadingCardToTagsMap.msg}`,
            )
        } else if (hasNoValue(allTags) || hasNoValue(allTagsMap) || hasNoValue(cardToTagsMap)) {
            return 'Loading tags...'
        } else {
            return RE.Container.col.top.left({style:{marginTop:'5px'}},{style:{marginTop:'5px'}},
                renderAddFilterButton(),
                renderSelectedFilters(),
            )
        }
    }

    return RE.Fragment({},
        renderComponentContent(),
        renderMessagePopup()
    )
}
