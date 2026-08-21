package com.liskovsoft.smartyoutubetv2.common.app.presenters.dialogs;

import android.content.Context;

import com.liskovsoft.mediaserviceinterfaces.data.MediaGroup;
import com.liskovsoft.mediaserviceinterfaces.data.MediaItem;
import com.liskovsoft.sharedutils.helpers.MessageHelpers;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.ChannelUploadsPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.PlaybackPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.SearchPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.base.BasePresenter;
import com.liskovsoft.smartyoutubetv2.common.misc.MediaServiceManager;
import com.liskovsoft.smartyoutubetv2.common.utils.LoadingManager;

public class VideoActionPresenter extends BasePresenter<Void> {
    private static final String TAG = VideoActionPresenter.class.getSimpleName();

    private VideoActionPresenter(Context context) {
        super(context);
    }

    public static VideoActionPresenter instance(Context context) {
        return new VideoActionPresenter(context);
    }

    public void apply(Video item) {
        if (item == null) {
            return;
        }

        boolean playlistLike = item.hasPlaylist() || item.hasNestedItems() || item.hasReloadPageKey() || item.isMix();
        if (playlistLike && (item.belongsToMusic() || isLiveGroup(item) || item.isMix())) {
            startFistPlaylistItem(item);
            return;
        }

        // Show playlist contents in channel instead of instant playback
        if (item.hasVideo() && !item.isBadgePlaylistInChannel()) {
            PlaybackPresenter.instance(getContext()).openVideo(item);
        } else if (item.hasChannel() || (item.belongsToChannelUploads() && item.hasNestedItems())) {
            MediaServiceManager.chooseChannelPresenter(getContext(), item);
        } else if (playlistLike) {
            ChannelUploadsPresenter.instance(getContext()).openChannel(item);
        } else if (item.isChapter) {
            PlaybackPresenter.instance(getContext()).setPosition(item.startTimeMs);
        } else if (item.searchQuery != null ) {
            SearchPresenter.instance(getContext()).onSearch(item.searchQuery);
        } else {
            MessageHelpers.showMessage(getContext(), "Video item doesn't contain needed data!");
        }
    }

    private void startFistPlaylistItem(Video item) {
        LoadingManager.showLoading(getContext(), true);
        final boolean[] received = { false };
        ChannelUploadsPresenter.instance(getContext()).obtainGroup(item, mediaGroup -> {
            received[0] = true;
            LoadingManager.showLoading(getContext(), false);
            MediaItem playable = firstPlayable(mediaGroup);
            if (playable != null) {
                PlaybackPresenter.instance(getContext()).openVideo(Video.from(playable));
            } else {
                ChannelUploadsPresenter.instance(getContext()).openChannel(item);
            }
        },
        e -> {
            LoadingManager.showLoading(getContext(), false);
            ChannelUploadsPresenter.instance(getContext()).openChannel(item);
        },
        () -> {
            LoadingManager.showLoading(getContext(), false);
            if (!received[0]) {
                ChannelUploadsPresenter.instance(getContext()).openChannel(item);
            }
        });
    }

    private static boolean isLiveGroup(Video item) {
        return item.getGroup() != null && item.getGroup().getType() == MediaGroup.TYPE_LIVE;
    }

    private static MediaItem firstPlayable(MediaGroup mediaGroup) {
        if (mediaGroup == null || mediaGroup.getMediaItems() == null) {
            return null;
        }
        for (MediaItem mediaItem : mediaGroup.getMediaItems()) {
            if (mediaItem != null && mediaItem.getVideoId() != null) {
                return mediaItem;
            }
        }
        return null;
    }
}
