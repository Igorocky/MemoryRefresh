"use strict";

const AVAILABLE_TRANSLATE_CARD_FILTERS = {
    SEARCH_IN_ACTIVE:'SEARCH_IN_ACTIVE',
    INCLUDE_TAGS:'INCLUDE_TAGS',
    EXCLUDE_TAGS:'EXCLUDE_TAGS',
    CREATED_ON_OR_AFTER:'CREATED_ON_OR_AFTER',
    CREATED_ON_OR_BEFORE:'CREATED_ON_OR_BEFORE',
    NATIVE_TEXT_LENGTH:'NATIVE_TEXT_LENGTH',
    NATIVE_TEXT_CONTAINS:'NATIVE_TEXT_CONTAINS',
    FOREIGN_TEXT_LENGTH:'FOREIGN_TEXT_LENGTH',
    FOREIGN_TEXT_CONTAINS:'FOREIGN_TEXT_CONTAINS',
    SORT_BY:'SORT_BY',
}

const AVAILABLE_TRANSLATE_CARD_SORT_BY = {
    TIME_CREATED:'TIME_CREATED',
}

const AVAILABLE_TRANSLATE_CARD_SORT_DIR = {
    ASC:'ASC',
    DESC:'DESC',
}

const TranslateCardFilterCmp = ({onSubmit}) => {
    const {renderMessagePopup, showMessage, confirmAction, showError, showDialog} = useMessagePopup()
    const af = AVAILABLE_TRANSLATE_CARD_FILTERS
    const sb = AVAILABLE_TRANSLATE_CARD_SORT_BY
    const sd = AVAILABLE_TRANSLATE_CARD_SORT_DIR

    const [allTags, setAllTags] = useState(null)
    const [allTagsMap, setAllTagsMap] = useState(null)
    const [errorLoadingTags, setErrorLoadingTags] = useState(null)

    const [cardToTagsMap, setCardToTagsMap] = useState(null)
    const [errorLoadingCardToTagsMap, setErrorLoadingCardToTagsMap] = useState(null)

    const [filtersSelected, setFiltersSelected] = useState([af.SORT_BY])
    const [focusedFilter, setFocusedFilter] = useState(filtersSelected[0])

    const [searchInActive, setSearchInActive] = useState(true)

    const [tagsToInclude, setTagsToInclude] = useState([])
    const [tagsToExclude, setTagsToExclude] = useState([])
    const [remainingTags, setRemainingTags] = useState([])

    const [createdOnOrAfter, setCreatedOnOrAfter] = useState(new Date())
    const [createdOnOrBefore, setCreatedOnOrBefore] = useState(new Date())

    const [nativeTextMinLength, setNativeTextMinLength] = useState(null)
    const [nativeTextMaxLength, setNativeTextMaxLength] = useState(null)
    const [nativeTextContains, setNativeTextContains] = useState('')
    const [foreignTextMinLength, setForeignTextMinLength] = useState(null)
    const [foreignTextMaxLength, setForeignTextMaxLength] = useState(null)
    const [foreignTextContains, setForeignTextContains] = useState('')

    const [sortBy, setSortBy] = useState(sb.TIME_CREATED)
    const [sortDir, setSortDir] = useState(sd.ASC)

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

    function renderSearchInActive() {
        const filterName = af.SEARCH_IN_ACTIVE
        const minimized = filterName !== focusedFilter
        return RE.Container.col.top.left({},{},
            RE.Container.row.left.center({},{},
                iconButton({
                    iconName:'cancel',
                    onClick: () => {
                        removeFilter(filterName)
                        setSearchInActive(true)
                    }}
                ),
                RE.span({style:{paddingRight:'10px'}, onClick: () => setFocusedFilter(null)}, 'Active or paused:')
            ),
            RE.If(minimized, () => RE.span({style:{padding:'5px', color:'blue'}}, searchInActive ? 'Active' : 'Paused')),
            RE.IfNot(minimized, () => RE.FormControl({component:'fieldset', style:{marginLeft:'10px'}},
                RE.RadioGroup({row:true, value: searchInActive, onChange: event => setSearchInActive(event.target.value === 'true')},
                    RE.FormControlLabel({value:true, control:RE.Radio({}), label: 'Active'}),
                    RE.FormControlLabel({value:false, control:RE.Radio({}), label: 'Paused', style:{marginLeft:'10px'}}),
                )
            ))
        )
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

    function renderNativeTextContains() {
        const filterName = af.NATIVE_TEXT_CONTAINS
        const minimized = filterName !== focusedFilter
        return RE.Container.col.top.left({},{},
            RE.Container.row.left.center({},{},
                iconButton({
                    iconName:'cancel',
                    onClick: () => {
                        removeFilter(filterName)
                        setNativeTextContains('')
                    }
                }),
                RE.span({style:{paddingRight:'10px'}, onClick: () => setFocusedFilter(null)}, 'Native text contains:')
            ),
            RE.If(minimized, () => RE.span({style:{padding:'5px', color:'blue'}}, `"${nativeTextContains}"`)),
            RE.IfNot(minimized, () => RE.TextField({
                autoCorrect: 'off', autoCapitalize: 'off', spellCheck: 'false',
                value: nativeTextContains,
                label: 'Native text contains',
                variant: 'outlined',
                multiline: false,
                maxRows: 1,
                size: 'small',
                inputProps: {size:24},
                style: {margin:'5px'},
                onChange: event => {
                    const newText = event.nativeEvent.target.value
                    if (newText != nativeTextContains) {
                        setNativeTextContains(newText)
                    }
                },
            }))
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

    function renderForeignTextContains() {
        const filterName = af.FOREIGN_TEXT_CONTAINS
        const minimized = filterName !== focusedFilter
        return RE.Container.col.top.left({},{},
            RE.Container.row.left.center({},{},
                iconButton({
                    iconName:'cancel',
                    onClick: () => {
                        removeFilter(filterName)
                        setForeignTextContains('')
                    }
                }),
                RE.span({style:{paddingRight:'10px'}, onClick: () => setFocusedFilter(null)}, 'Foreign text contains:')
            ),
            RE.If(minimized, () => RE.span({style:{padding:'5px', color:'blue'}}, `"${foreignTextContains}"`)),
            RE.IfNot(minimized, () => RE.TextField({
                autoCorrect: 'off', autoCapitalize: 'off', spellCheck: 'false',
                value: foreignTextContains,
                label: 'Foreign text contains',
                variant: 'outlined',
                multiline: false,
                maxRows: 1,
                size: 'small',
                inputProps: {size:24},
                style: {margin:'5px'},
                onChange: event => {
                    const newText = event.nativeEvent.target.value
                    if (newText != foreignTextContains) {
                        setForeignTextContains(newText)
                    }
                },
            }))
        )
    }

    function renderSortBy() {
        const filterName = af.SORT_BY
        return RE.Container.col.top.left({},{},
            RE.Container.row.left.center({},{},
                iconButton({
                    iconName:'cancel',
                    onClick: () => {
                        removeFilter(filterName)
                        setSortBy(sb.TIME_CREATED)
                        setSortDir(sd.ASC)
                    }
                }),
                RE.span({style:{paddingRight:'10px'}, onClick: () => setFocusedFilter(null)}, 'Sort by:')
            ),
            re(SortBySelector,{
                possibleParams: {[sb.TIME_CREATED]:{displayName:'Date created'}},
                selectedParam: sortBy,
                onParamSelected: newSortBy => setSortBy(newSortBy),
                possibleDirs: {[sd.ASC]:{displayName:'A-Z'}, [sd.DESC]:{displayName:'Z-A'}},
                selectedDir: sortDir,
                onDirSelected: newSortDir => setSortDir(newSortDir),
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
                : (filterName === af.NATIVE_TEXT_CONTAINS) ? renderNativeTextContains()
                : (filterName === af.FOREIGN_TEXT_LENGTH) ? renderForeignTextLength()
                : (filterName === af.FOREIGN_TEXT_CONTAINS) ? renderForeignTextContains()
                : (filterName === af.SEARCH_IN_ACTIVE) ? renderSearchInActive()
                : (filterName === af.SORT_BY) ? renderSortBy()
                : `unexpected filter - ${filterName}`
            ))
        )
    }

    function renderAvailableFilterListItem({filterName, filterDisplayName, resolve}) {
        return RE.If(!filtersSelected.includes(filterName), () => RE.Fragment({},
            RE.ListItem(
                {button:true, onClick: () => resolve(filterName)},
                RE.ListItemText({}, filterDisplayName)
            ),
            RE.Divider({})
        ))
    }

    function renderListOfAvailableFilters(resolve) {
        return RE.List({},
            renderAvailableFilterListItem({filterName: af.SEARCH_IN_ACTIVE, filterDisplayName: 'Active or Paused', resolve}),
            renderAvailableFilterListItem({filterName: af.INCLUDE_TAGS, filterDisplayName: 'Tags to include', resolve}),
            renderAvailableFilterListItem({filterName: af.EXCLUDE_TAGS, filterDisplayName: 'Tags to exclude', resolve}),
            renderAvailableFilterListItem({filterName: af.CREATED_ON_OR_AFTER, filterDisplayName: 'Created on or after', resolve}),
            renderAvailableFilterListItem({filterName: af.CREATED_ON_OR_BEFORE, filterDisplayName: 'Created on or before', resolve}),
            renderAvailableFilterListItem({filterName: af.NATIVE_TEXT_LENGTH, filterDisplayName: 'Native text length', resolve}),
            renderAvailableFilterListItem({filterName: af.NATIVE_TEXT_CONTAINS, filterDisplayName: 'Native text contains', resolve}),
            renderAvailableFilterListItem({filterName: af.FOREIGN_TEXT_LENGTH, filterDisplayName: 'Foreign text length', resolve}),
            renderAvailableFilterListItem({filterName: af.FOREIGN_TEXT_CONTAINS, filterDisplayName: 'Foreign text contains', resolve}),
            renderAvailableFilterListItem({filterName: af.SORT_BY, filterDisplayName: 'Sort by', resolve}),
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
