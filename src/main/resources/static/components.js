/**
 * Created by haminata on 04/08/2018.
 */

const e = React.createElement;

Object.assign(e, ReactDOMFactories);

const VIEW_FORMAT = {
    LIST_ITEM: 'list_item',
    GRID_ITEM: 'grid_item',
    FULL: 'full'
};

class ModelCollection extends React.Component {

    constructor(props) {
        super(props || {})
        this.viewFormat = this.props.viewFormat || VIEW_FORMAT.GRID_ITEM
    }

    render() {
        let elems = _.map(this.props.modelsProps || [], (prop) => {
            let key = `${this.props.model.name}_${prop.id}`;
            let el = e(this.props.model, _.merge({key, viewFormat: this.viewFormat}, prop));

            let style = {};

            if (this.viewFormat === VIEW_FORMAT.GRID_ITEM) {
                return el;//e('div', {key, className: 'col-md-4 col-lg-4 col-sm-6', style}, [el]);
            } else {
                return e.li({key, className: "list-group-item"}, [el])
            }

        });


        let opts = _.omit(this.props, ['modelsProps', 'model'])
        if (this.viewFormat === VIEW_FORMAT.GRID_ITEM) {
            return e.div(_.merge(opts, {className: 'container'}),
                e.div({className: 'card-columns'}, elems)
            );
        } else {
            return e.ul(_.merge(opts, {className: 'list-group list-group-flush'}), elems);
        }
    }

}

class Navbar extends React.Component {

    onCreateMusicroom() {
        app.setState({
            newMusicroom: _.merge(app.state.newMusicroom || {}, {
                viewFormat: VIEW_FORMAT.FULL,
                editMode: true
            })
        })
    }

    onSignIn() {
        console.log('[Navbar] sign in')
    }

    onSignUp() {
        console.log('[Navbar] sign up')
    }

    componentDidMount() {
        window.appNavbar = this
    }

    onBrandClick() {
        app.newMusicroom = null
        app.selectedRoom = null
    }

    render() {
        let children = [
            e.a({className: 'navbar-brand', href: '#', onClick: this.onBrandClick.bind(this)}, app.name),
            e.button({
                className: 'navbar-toggler',
                type: "button",
                'data-toggle': "collapse",
                'data-target': "#navbarText"
            }, [
                e.span({className: 'navbar-toggler-icon'})
            ]),
            //<button class="btn btn-outline-success my-2 my-sm-0 ml-2" data-emit-click="create_room">Create Room</button>
            e.ul({className: 'nav nav-tab navbar-nav mr-auto'}),
            e.button({
                className: 'btn btn-outline-primary my-2 my-sm-0 ml-2',
                onClick: this.onCreateMusicroom.bind(this)
            }, 'Create Room'),
            e.a({
                href: '/spotify_login',
                role: "button",
                className: 'btn btn-outline-success my-2 my-sm-0 ml-2',
                onClick: this.onSignIn.bind(this)
            }, 'Connect to Spotify'),
            e.a({
                href: '/login',
                className: 'btn btn-link my-2 my-sm-0 ml-2',
                onClick: this.onSignIn.bind(this)
            }, 'Sign In'),
            e.span({className: 'text-muted'}, 'Or'),
            e.a({
                href: '/registration.html',
                className: 'btn btn-link my-2 my-sm-0 ml-2',
                onClick: this.onSignUp.bind(this)
            }, 'Sign Up')
        ];
        return e.nav({className: 'nav navbar navbar-dark navbar-expand-sm bg-dark'}, e.div({className: 'container'}, children))
    }
}

class Toolbar extends React.Component {

    constructor(props) {
        super(props || {});
        this.state = Object.assign({placeholder: 'Search music rooms', dropdownModels: []}, this.props);
        this.input = React.createRef();
        this.dropdown = React.createRef();
        this.container = React.createRef();

        this.drowdownFocused = false
    }

    get searchPlaceholder() {
        return this.state.placeholder || ''
    }

    componentDidMount() {
        window.appToolbar = this
        console.log('[input]', this.input.current)
        let $elem = $(this.input.current)

        $elem.on('focus', () => {
            if(_.isEmpty(this.state.dropdownModels) || ! app.musicroomView) return
            $(this.dropdown.current).show()
        })

    }

    onChange() {
        let term = this.input.current.value
        //console.log('[input]', );
        if (app.musicroomView){
            app.musicroomView.setState({searchQuerySong: this.input.current.value})

            if(!_.isEmpty(term)) {
                app.searchTracks(term).then((res) => {

                    //searchTracks.cancel()

                    console.log('[searchTracks]', res)
                    let tracks = _.map(res.tracks.items, (i) => {
                        let imgUrl = i.album.images[2].url;
                        return _.merge({title: i.name,
                            spotifyUri: i.uri,
                            thumbnailUrl: imgUrl,
                            thumbnail_url: imgUrl,
                            trackId: i.id,
                            track_id: i.id,
                            artistName: i.artists[0].name,
                            artist_name: i.artists[0].name,
                        }, i)
                    })
                    this.setState({dropdownModels: tracks})
                })
            } else {
                this.setState({dropdownModels: []})
            }

        } else {
            app.setState({searchQueryMusicroom: this.input.current.value});
        }
    }

    render() {
        let style = {}//{maxHeight: '78px'}
        return e.div({className: "alert rounded-0", style, id: 'toolbar', role: "alert"}, [
            e.div({className: 'container'}, [
                e.div({className: 'input-group input-group-lg w-100', ref: this.container, style: {position: 'relative'}}, [
                    e.span({className: 'input-group-prepend border-light'}, [
                        e.div({className: 'input-group-text border bg-light'}, [
                            e.i({className: 'fa fa-search'}),
                        ])
                    ]),
                    e.input({
                        type: 'text',
                        className: 'form-control py-2 border-left-0 border rounded-right bg-light',
                        placeholder: this.searchPlaceholder,
                        ref: this.input,
                        //value: app.musicroomView ? app.state.searchQueryMusicroom : app.state.searchQuerySong,
                        onChange: this.onChange.bind(this)
                    }),
                    e.div({
                            className: 'border rounded shadow-lg p-3 bg-white',
                            id: 'search_dropdown',
                            style: {
                                display: _.isEmpty(this.state.dropdownModels) ? 'none' : 'block',
                                position: 'absolute',
                                backgroundColor: 'white',
                                width: '100%',
                                top: 'calc(100% + 4px)',
                                maxHeight: '50vh',
                                overflow: 'scroll',
                                left: '0',
                                zIndex: 100
                            },
                            ref: this.dropdown,
                        },
                        e(ModelCollection, {
                            modelsProps: this.state.dropdownModels || [],
                            viewFormat: VIEW_FORMAT.LIST_ITEM,
                            model: Track,
                            style: {
                                width: '100%',
                            },
                        })),

                ])
            ])
        ]);
    }

}
