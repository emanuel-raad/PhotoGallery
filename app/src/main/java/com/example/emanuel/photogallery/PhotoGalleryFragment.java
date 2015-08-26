package com.example.emanuel.photogallery;

import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Emanuel on 22/08/2015.
 */
public class PhotoGalleryFragment extends Fragment {

    private RecyclerView mPhotoRecyclerView;
    private List<GalleryItem> mItems = new ArrayList<>();
    // Old way thread
    // private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;

    private int mPage;
    private int mReturnedItems;
    private int mNumberOfColumns;
    private int mRow;
    private List<Integer> mLoadedRows = new ArrayList<>();
    private boolean mLoading;
    private boolean mSearchMode;
    private int mVisibleItemCount, mTotalItemCount, mPastVisibleItems;

    private static final String TAG = "PhotoGalleryFragment";
    private static final String PAGE_NUMBER = "page_number";
    private static final String LOADED_PICS = "loaded_pics";
    private static final int PRELOADED_IMAGES = 10;
    private static final int PRELOADED_ROWS = 3;
    private static final int COLUMN_WIDTH = 360;


    public static PhotoGalleryFragment newInstance() {
        return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);

        if (savedInstanceState != null) {
            mPage = (int) savedInstanceState.getSerializable(PAGE_NUMBER);
            mReturnedItems = (int) savedInstanceState.getSerializable(LOADED_PICS);
        } else {
            mPage = 1;
            mReturnedItems = 0;
        }

        //updateItems();
        new FetchItemsTask(null, mPage).execute();

        // Not needed anymore because Picasso handles all the images downloads
        // Left as a reference

/*      Handler responseHandler = new Handler();
        mThumbnailDownloader = new ThumbnailDownloader<>(responseHandler);
        mThumbnailDownloader.setThumbnailDownloadListener(
                new ThumbnailDownloader.ThumbnailDownloadListener<PhotoHolder>() {
                    @Override
                    public void onThumbnailDownloaded(PhotoHolder photoHolder, Bitmap thumbnail) {
                        Drawable drawable = new BitmapDrawable(getResources(), thumbnail);
                        photoHolder.bindDrawable(drawable);
                    }
                }
        );

        mThumbnailDownloader.start();
        mThumbnailDownloader.getLooper();
        Log.i(TAG, "Background thread started");*/

        // Just debug stuff. Not actually needed
        Picasso.with(getActivity()).setIndicatorsEnabled(true);
        Picasso.with(getActivity()).setLoggingEnabled(false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //mThumbnailDownloader.quit();
        //Log.i(TAG, "Background thread destroyed.");
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_photo_gallery, menu);

        MenuItem searchItem = menu.findItem(R.id.menu_item_search);
        final SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.d(TAG, "QueryTextSubmit: " + query);
                QueryPreferences.setStoredQuery(getActivity(), query);
                mSearchMode = true;
                mItems.clear();
                mReturnedItems = 0;
                mLoadedRows.clear();
                mPhotoRecyclerView.scrollToPosition(0);
                updateItems();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Log.d(TAG, "QueryTextChange: " + newText);
                return false;
            }
        });

        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String query = QueryPreferences.getStoredQuery(getActivity());
                searchView.setQuery(query, false);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_clear:
                QueryPreferences.setStoredQuery(getActivity(), null);
                updateItems();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void updateItems() {
        String query = mSearchMode ? QueryPreferences.getStoredQuery(getActivity()) : null;
        new FetchItemsTask(query, mPage).execute();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_photo_gallery, container, false);

        mPhotoRecyclerView = (RecyclerView) view
                .findViewById(R.id.fragment_photo_gallery_recycler_view);

        mPhotoRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        mPhotoRecyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        int width = mPhotoRecyclerView.getMeasuredWidth();
                        mNumberOfColumns = width / COLUMN_WIDTH;
                        mPhotoRecyclerView.setLayoutManager(new GridLayoutManager(
                                getActivity(), mNumberOfColumns));
                        mPhotoRecyclerView.getLayoutManager().requestLayout();
                    }
                }
        );

        mPhotoRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                GridLayoutManager lm = (GridLayoutManager) recyclerView.getLayoutManager();

                mVisibleItemCount = lm.getChildCount();
                mTotalItemCount = lm.getItemCount();
                mPastVisibleItems = lm.findFirstVisibleItemPosition();
                mRow = (mVisibleItemCount + mPastVisibleItems) / mNumberOfColumns;

                //This is really messy
                for (int i = 0; i <= PRELOADED_ROWS; i++) {
                    int row = mRow + i;
                    if (!mLoadedRows.contains(row)) {
                        for (int j = 0; j < mNumberOfColumns; j++) {
                            int nextPic = mVisibleItemCount + mPastVisibleItems + j;
                            if (!mLoading) {
                                try {
                                    Picasso.with(getActivity()).load(mItems.get(i + nextPic).getUrl()).fetch();
                                    Log.d(TAG, "Preloaded picture #: " + (i + nextPic) + " URL: " + mItems.get(nextPic).getUrl());
                                } catch (IndexOutOfBoundsException e) {
                                    mLoading = false;
                                    mPage += 1;
                                    updateItems();
                                    Log.d(TAG, "Loading next page: ", e);
                                }
                            }
                        }
                        mLoadedRows.add(row);
                    }
                }
            }
        });

        setupAdapter();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // More memories of the past
        //mThumbnailDownloader.clearQueue();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(PAGE_NUMBER, mPage);
        outState.putSerializable(LOADED_PICS, mReturnedItems);
    }

    private void setupAdapter() {
        if (isAdded()) {
            mPhotoRecyclerView.setAdapter(new PhotoAdapter(mItems));
        }
    }

    private class PhotoHolder extends RecyclerView.ViewHolder {
        private ImageView mItemImageView;

        public PhotoHolder(View itemView) {
            super(itemView);

            mItemImageView = (ImageView) itemView
                    .findViewById(R.id.fragment_photo_gallery_image_view);
        }

        // Old way. Not used anymore
        public void bindDrawable(Drawable drawable) {
            mItemImageView.setImageDrawable(drawable);
        }

        // Picasso way
        public void bindGalleryItem(GalleryItem galleryItem) {
            Picasso.with(getActivity())
                    .load(galleryItem.getUrl())
                    .placeholder(R.drawable.ic_refresh)
                    .noFade()
                    .into(mItemImageView);
        }
    }

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder> {

        private List<GalleryItem> mGalleryItems;

        public PhotoAdapter(List<GalleryItem> galleryItems) {
            mGalleryItems = galleryItems;
        }

        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View view = inflater.inflate(R.layout.gallery_item, viewGroup, false);
            return new PhotoHolder(view);
        }

        @Override
        public void onBindViewHolder(PhotoHolder photoHolder, int i) {
            GalleryItem galleryItem = mGalleryItems.get(i);
            //Old way
            /*Drawable placeholder = getResources().getDrawable(R.drawable.ic_refresh);
            photoHolder.bindDrawable(placeholder);
            mThumbnailDownloader.queueThumbnail(photoHolder, galleryItem.getUrl());*/

            //Picasso way
            photoHolder.bindGalleryItem(galleryItem);
        }

        @Override
        public int getItemCount() {
            return mGalleryItems.size();
        }
    }

    private class FetchItemsTask extends AsyncTask<Void, Void, List<GalleryItem>> {
        private String mQuery;
        private int mPage;

        public FetchItemsTask(String query, int page) {
            mQuery = query;
            mPage = page;
        }

        public FetchItemsTask(int page) {
            this(null, page);
        }

        //Runs on a background thread
        @Override
        protected List<GalleryItem> doInBackground(Void... voids) {
            if (mQuery == null) {
                return new FlickrFetchr().fetchRecentPhotos(mPage);
            } else {
                return new FlickrFetchr().searchPhotos(mQuery, mPage);
            }
        }

        //Runs on the main thread
        @Override
        protected void onPostExecute(List<GalleryItem> galleryItems) {
            mReturnedItems += galleryItems.size();
            mItems.addAll(galleryItems);
            mLoading = false;
            mPhotoRecyclerView.getAdapter().notifyDataSetChanged();
        }
    }
}
