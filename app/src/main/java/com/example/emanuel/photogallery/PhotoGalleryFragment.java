package com.example.emanuel.photogallery;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
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
    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;

    private Integer mPage;
    private boolean mLoading;
    private int mVisibleItemCount, mTotalItemCount, mPastVisibleItems;

    private static final String TAG = "PhotoGalleryFragment";
    private static final String PAGE_NUMBER = "page_number";
    private static final int PRELOADED_IMAGES = 10;
    private static final int COLUMN_WIDTH = 360;


    public static PhotoGalleryFragment newInstance() {
        return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        if (savedInstanceState != null) {
            mPage = (Integer) savedInstanceState.getSerializable(PAGE_NUMBER);
        } else {
            mPage = 1;
        }

        new FetchItemsTask().execute();

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
        Picasso.with(getActivity()).setLoggingEnabled(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //mThumbnailDownloader.quit();
        //Log.i(TAG, "Background thread destroyed.");
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
                        int numberOfColumns = width / COLUMN_WIDTH;
                        mPhotoRecyclerView.setLayoutManager(new GridLayoutManager(
                                getActivity(), numberOfColumns));
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

                for (int i = 0; i <= PRELOADED_IMAGES; i++) {
                    int nextPic = mVisibleItemCount + mPastVisibleItems + i + 1;
                    if (!mLoading) {
                        if (nextPic >= (mTotalItemCount)) {
                            mLoading = false;
                            nextPage();
                            Log.d(TAG, "Loading next page");
                        } else {
                            Picasso.with(getActivity()).load(mItems.get(nextPic).getUrl()).fetch();
                            Log.d(TAG, "Preloaded picture #: " + nextPic + " URL: " + mItems.get(nextPic).getUrl());
                        }
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
    }

    private void nextPage() {
        mPage += 1;
        new FetchItemsTask().execute();
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

        //Runs on a background thread
        @Override
        protected List<GalleryItem> doInBackground(Void... voids) {
            return new FlickrFetchr().fetchItems(Integer.toString(mPage));
        }

        //Runs on the main thread
        @Override
        protected void onPostExecute(List<GalleryItem> galleryItems) {
            mItems.addAll(galleryItems);
            mLoading = false;
            mPhotoRecyclerView.getAdapter().notifyDataSetChanged();
        }
    }
}
