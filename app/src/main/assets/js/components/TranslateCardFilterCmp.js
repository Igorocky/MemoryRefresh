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

const TranslateCardFilterCmp = ({
                                    allTags, allTagsMap, onSubmit, minimized,
                                    submitButtonIconName = 'search',
                                    allowedFilters, defaultFilters = [AVAILABLE_TRANSLATE_CARD_FILTERS.INCLUDE_TAGS, AVAILABLE_TRANSLATE_CARD_FILTERS.EXCLUDE_TAGS],
                                    cardUpdateCounter
                                }) => {
    const {renderMessagePopup, showError, showDialog} = useMessagePopup()
    const af = AVAILABLE_TRANSLATE_CARD_FILTERS
    const sb = AVAILABLE_TRANSLATE_CARD_SORT_BY
    const sd = AVAILABLE_TRANSLATE_CARD_SORT_DIR

    const [cardToTagsMap, setCardToTagsMap] = useState(null)
    const [errorLoadingCardToTagsMap, setErrorLoadingCardToTagsMap] = useState(null)

    const [filtersSelected, setFiltersSelected] = useState(defaultFilters)
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
        const res = await be.getCardToTagMapping()
        if (res.err) {
            setErrorLoadingCardToTagsMap(res.err)
            showError(res.err)
        } else {
            setCardToTagsMap(res.data)
        }
    }, [cardUpdateCounter])

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

    function createSearchInActiveFilterObject() {
        const filterName = af.SEARCH_IN_ACTIVE
        const minimized = filterName !== focusedFilter
        return {
            [filterName]: {
                displayName: 'Active or Paused',
                render: () => RE.Container.col.top.left({},{},
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
                ),
                renderMinimized: () => `Search in: ${searchInActive ? 'Active' : 'Paused'}`,
                getFilterValues: () => ({paused: !searchInActive})
            }
        }
    }

    function createTagsToIncludeFilterObject() {
        const filterName = af.INCLUDE_TAGS
        const minimized = filterName !== focusedFilter
        return {
            [filterName]: {
                displayName: 'Tags to include',
                render: () => RE.Container.col.top.left({},{},
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
                        minimized,
                        autoFocus: true,
                    })
                ),
                renderMinimized: () => RE.Fragment({},
                    'Include: ',
                    renderListOfTags({tags: tagsToInclude, color:'blue'})
                ),
                getFilterValues: () => ({tagIdsToInclude: tagsToInclude.map(t=>t.id)})
            }
        }
    }

    function createTagsToExcludeFilterObject() {
        const filterName = af.EXCLUDE_TAGS
        const minimized = filterName !== focusedFilter
        return {
            [filterName]: {
                displayName: 'Tags to exclude',
                render: () => RE.Container.col.top.left({},{},
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
                        minimized,
                        autoFocus: true,
                    })
                ),
                renderMinimized: () => RE.Fragment({},
                    'Exclude: ',
                    renderListOfTags({tags: tagsToExclude, color:'red'})
                ),
                getFilterValues: () => ({tagIdsToExclude: tagsToExclude.map(t=>t.id)})
            }
        }
    }

    function createCreatedOnOrAfterFilterObject() {
        const filterName = af.CREATED_ON_OR_AFTER
        const minimized = filterName !== focusedFilter
        return {
            [filterName]: {
                displayName: 'Created on or after',
                render: () => RE.Container.col.top.left({},{},
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
                        minimized,
                    })
                ),
                renderMinimized: () => `Created on or after: ${createdOnOrAfter.getFullYear()} ${ALL_MONTHS[createdOnOrAfter.getMonth()]} ${createdOnOrAfter.getDate()}`,
                getFilterValues: () => ({createdFrom: startOfDay(createdOnOrAfter).getTime()})
            }
        }
    }

    function createCreatedOnOrBeforeFilterObject() {
        const filterName = af.CREATED_ON_OR_BEFORE
        const minimized = filterName !== focusedFilter
        return {
            [filterName]: {
                displayName: 'Created on or before',
                render: () => RE.Container.col.top.left({},{},
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
                        minimized,
                    })
                ),
                renderMinimized: () => `Created on or before: ${createdOnOrBefore.getFullYear()} ${ALL_MONTHS[createdOnOrBefore.getMonth()]} ${createdOnOrBefore.getDate()}`,
                getFilterValues: () => ({createdTill: addDays(startOfDay(createdOnOrBefore),1).getTime()})
            }
        }
    }

    function createNativeTextLengthFilterObject() {
        const filterName = af.NATIVE_TEXT_LENGTH
        const minimized = filterName !== focusedFilter
        const parameterName = '(native text length)'
        return {
            [filterName]: {
                displayName: 'Native text length',
                render: () => RE.Container.col.top.left({},{},
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
                        parameterName: parameterName,
                        minimized,
                    })
                ),
                renderMinimized: () => `${(hasValue(nativeTextMinLength) ? nativeTextMinLength : 0) + ' \u2264 '}${parameterName}${hasValue(nativeTextMaxLength) ? ' \u2264 ' + nativeTextMaxLength : ''}`,
                getFilterValues: () => ({
                    textToTranslateLengthGreaterThan: hasValue(nativeTextMinLength) ? nativeTextMinLength - 1 : null,
                    textToTranslateLengthLessThan: hasValue(nativeTextMaxLength) ? (nativeTextMaxLength-0) + 1 : null,
                })
            }
        }
    }

    function createNativeTextContainsFilterObject() {
        const filterName = af.NATIVE_TEXT_CONTAINS
        const minimized = filterName !== focusedFilter
        return {
            [filterName]: {
                displayName: 'Native text contains',
                render: () => RE.Container.col.top.left({},{},
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
                ),
                renderMinimized: () => `Native text contains: "${nativeTextContains}"`,
                getFilterValues: () => ({textToTranslateContains: nativeTextContains})
            }
        }
    }

    function createForeignTextLengthFilterObject() {
        const filterName = af.FOREIGN_TEXT_LENGTH
        const minimized = filterName !== focusedFilter
        const parameterName = '(foreign text length)'
        return {
            [filterName]: {
                displayName: 'Foreign text length',
                render: () => RE.Container.col.top.left({},{},
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
                        parameterName: parameterName,
                        minimized,
                    })
                ),
                renderMinimized: () => `${(hasValue(foreignTextMinLength) ? foreignTextMinLength : 0) + ' \u2264 '}${parameterName}${hasValue(foreignTextMaxLength) ? ' \u2264 ' + foreignTextMaxLength : ''}`,
                getFilterValues: () => ({
                    translationLengthGreaterThan: hasValue(foreignTextMinLength) ? foreignTextMinLength - 1 : null,
                    translationLengthLessThan: hasValue(foreignTextMaxLength) ? (foreignTextMaxLength-0) + 1 : null,
                })
            }
        }
    }

    function createForeignTextContainsFilterObject() {
        const filterName = af.FOREIGN_TEXT_CONTAINS
        const minimized = filterName !== focusedFilter
        return {
            [filterName]: {
                displayName: 'Foreign text contains',
                render: () => RE.Container.col.top.left({},{},
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
                ),
                renderMinimized: () => `Foreign text contains: "${foreignTextContains}"`,
                getFilterValues: () => ({translationContains: foreignTextContains})
            }
        }
    }

    function createSortByFilterObject() {
        const filterName = af.SORT_BY
        const minimized = filterName !== focusedFilter
        const possibleParams = {[sb.TIME_CREATED]:{displayName:'Date created'}}
        const possibleDirs = {[sd.ASC]:{displayName:'A-Z'}, [sd.DESC]:{displayName:'Z-A'}}
        return {
            [filterName]: {
                displayName: 'Sort by',
                render: () => RE.Container.col.top.left({},{},
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
                        possibleParams: possibleParams,
                        selectedParam: sortBy,
                        onParamSelected: newSortBy => setSortBy(newSortBy),
                        possibleDirs: possibleDirs,
                        selectedDir: sortDir,
                        onDirSelected: newSortDir => setSortDir(newSortDir),
                        minimized,
                    })
                ),
                renderMinimized: () => `Sort by: ${possibleParams[sortBy].displayName} ${possibleDirs[sortDir].displayName}`,
                getFilterValues: () => ({sortBy, sortDir})
            }
        }
    }

    const allFilterObjects = {
        ...createSearchInActiveFilterObject(),
        ...createTagsToIncludeFilterObject(),
        ...createTagsToExcludeFilterObject(),
        ...createCreatedOnOrAfterFilterObject(),
        ...createCreatedOnOrBeforeFilterObject(),
        ...createNativeTextLengthFilterObject(),
        ...createNativeTextContainsFilterObject(),
        ...createForeignTextLengthFilterObject(),
        ...createForeignTextContainsFilterObject(),
        ...createSortByFilterObject(),
    }

    function renderSelectedFilters() {
        return RE.Container.col.top.left({style:{marginTop:'5px'}},{style:{marginTop:'5px'}},
            filtersSelected.map(filterName => RE.Paper({onClick: () => focusedFilter !== filterName ? setFocusedFilter(filterName) : null},
                allFilterObjects[filterName].render()
            ))
        )
    }

    function renderAvailableFilterListItem({filterName, filterDisplayName, resolve}) {
        return RE.If(!filtersSelected.includes(filterName), () => RE.Fragment({key:filterName},
            RE.ListItem(
                {key:filterName, button:true, onClick: () => resolve(filterName)},
                RE.ListItemText({}, filterDisplayName)
            ),
            RE.Divider({key:filterName+'-d'})
        ))
    }

    function renderListOfAvailableFilters(resolve) {
        let filterNames = hasValue(allowedFilters)
            ? allowedFilters
            : [
                af.INCLUDE_TAGS,
                af.EXCLUDE_TAGS,
                af.SEARCH_IN_ACTIVE,
                af.CREATED_ON_OR_AFTER,
                af.CREATED_ON_OR_BEFORE,
                af.NATIVE_TEXT_LENGTH,
                af.NATIVE_TEXT_CONTAINS,
                af.FOREIGN_TEXT_LENGTH,
                af.FOREIGN_TEXT_CONTAINS,
                af.SORT_BY,
            ]
        return RE.List({},
            filterNames.map(filterName =>
                renderAvailableFilterListItem({filterName, filterDisplayName: allFilterObjects[filterName].displayName, resolve})
            )
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

    function getEffectiveSelectedFilterNames() {
        return filtersSelected
            .filter(filterName => (filterName !== af.INCLUDE_TAGS || tagsToInclude.length) && (filterName !== af.EXCLUDE_TAGS || tagsToExclude.length))
    }

    function getSelectedFilter() {
        return getEffectiveSelectedFilterNames()
            .map(filterName => allFilterObjects[filterName])
            .reduce((acc,elem) => ({...acc,...elem.getFilterValues()}), {})
    }

    function renderComponentContent() {
        if (errorLoadingCardToTagsMap) {
            return RE.Fragment({},
                `An error occurred during loading of card to tags mapping: [${errorLoadingCardToTagsMap.code}] - ${errorLoadingCardToTagsMap.msg}`,
            )
        } else if (hasNoValue(allTags) || hasNoValue(allTagsMap) || hasNoValue(cardToTagsMap)) {
            return 'Loading tags...'
        } else {
            return RE.Container.col.top.left({style:{marginTop:'5px'}},{style:{marginTop:'5px'}},
                RE.Container.row.left.center({},{},
                    renderAddFilterButton(),
                    iconButton({iconName:submitButtonIconName, onClick: () => onSubmit(getSelectedFilter())})
                ),
                renderSelectedFilters(),
            )
        }
    }

    if (minimized) {
        const filtersToRender = getEffectiveSelectedFilterNames()
        return RE.Paper({style:{padding:'5px'}},
            filtersToRender.length ? RE.Container.col.top.left({},{},
                filtersToRender.map(filterName => allFilterObjects[filterName].renderMinimized())
            ) : 'All cards'
        )
    } else {
        return RE.Fragment({},
            renderComponentContent(),
            renderMessagePopup()
        )
    }
}
