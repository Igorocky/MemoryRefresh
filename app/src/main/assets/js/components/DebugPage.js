"use strict";

function DebugPage({openView,setPageTitle}) {
    return RE.Container.col.top.center({style:{marginTop:'100px'}},{style:{marginTop:'10px'}},
        `navigator.userAgent: ${navigator.userAgent}`,
        RE.Button({variant:"contained", onClick: () => be.debug()}, 'Debug'),
        RE.Button({variant:"contained", onClick: () => openView(HOME_PAGE_VIEW)}, 'Go to home page'),
    )
}
