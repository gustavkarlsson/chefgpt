# Bottom-sheet screens via a SceneStrategy

The app renders screens that implement the `Screen.BottomSheet` marker interface as a
modal bottom sheet layered over the previous screen, rather than as a full-screen
push or a hand-rolled state overlay outside navigation. A custom navigation3
`SceneStrategy` (`BottomSheetSceneStrategy`) turns a bottom-sheet entry into a
`BottomSheetScene` (`OverlayScene`) whose content is a Material 3 `ModalBottomSheet`;
the entries it overlays are rendered by the single-pane fallback beneath it.

Chosen over a plain state-driven `ModalBottomSheet` outside the back stack because a
sheet must stay a first-class navigation entry: it participates in the back stack and
predictive back, gets its own `ViewModelStore` and lifecycle via the existing
`rememberViewModelStoreNavEntryDecorator`, and is dismissed with `Navigator.pop()`.
The marker interface is the source of truth for "is this a sheet"; the entry provider
maps it to a metadata key, since `SceneStrategy` only sees an entry's `metadata`, not
its key.
