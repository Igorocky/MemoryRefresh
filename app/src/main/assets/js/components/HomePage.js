"use strict";

function HomePage({openView,setPageTitle}) {
    const [availableViews, setAvailableViews] = useState([
        {name:DEBUG_VIEW},
        {name:PAGE_1_VIEW},
        {name:PAGE_2_VIEW},
    ])

    useEffect(() => {
        setPageTitle('MemoryRefresh')
    }, [])

    function renderListOfAvailableViews() {
        return RE.List({component:"nav"},
            availableViews.map((view,idx) => RE.ListItem(
                {
                    key:idx,
                    button:true,
                    onClick: () => openView(view.name)
                },
                RE.ListItemText({}, view.name)
            ))
        )
    }

    return RE.Container.col.top.center({style:{marginTop:'200px'}},{},
        renderListOfAvailableViews()
    )
}
