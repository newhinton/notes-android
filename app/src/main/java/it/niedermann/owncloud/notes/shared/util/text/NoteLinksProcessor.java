package it.niedermann.owncloud.notes.shared.util.text;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NoteLinksProcessor extends TextProcessor {

    private static final String TAG = NoteLinksProcessor.class.getSimpleName();
    public static final String RELATIVE_LINK_WORKAROUND_PREFIX = "https://nextcloudnotes/notes/";

    @VisibleForTesting
    private static final String linksThatLookLikeNoteLinksRegEx = "\\[[^]]*]\\((\\d+)\\)";
    private static final String replaceNoteRemoteIdsRegEx = "\\[([^\\]]*)\\]\\((%s)\\)";

    private Set<Long> existingNoteRemoteIds;

    public NoteLinksProcessor(Set<Long> existingNoteRemoteIds) {
        this.existingNoteRemoteIds = existingNoteRemoteIds;
    }

    /**
     * Replaces all links to other notes of the form `[<link-text>](<note-file-id>)`
     * in the markdown string with links to a dummy url.
     * <p>
     * Why is this needed?
     * See discussion in issue #623
     *
     * @return Markdown with all note-links replaced with dummy-url-links
     */
    @Override
    public String process(String s) {
        return replaceNoteLinksWithDummyUrls(s, existingNoteRemoteIds);
    }

    private static String replaceNoteLinksWithDummyUrls(String markdown, Set<Long> existingNoteRemoteIds) {
        Pattern noteLinkCandidates = Pattern.compile(linksThatLookLikeNoteLinksRegEx);
        Matcher matcher = noteLinkCandidates.matcher(markdown);

        Set<String> noteRemoteIdsToReplace = new HashSet<>();
        while (matcher.find()) {
            String presumedNoteId = matcher.group(1);
            try {
                if (presumedNoteId != null && existingNoteRemoteIds.contains(Long.parseLong(presumedNoteId))) {
                    noteRemoteIdsToReplace.add(presumedNoteId);
                }
            } catch (NumberFormatException e) {
                Log.w(TAG, e);
            }
        }

        String noteRemoteIdsCondition = TextUtils.join("|", noteRemoteIdsToReplace);
        Pattern replacePattern = Pattern.compile(String.format(replaceNoteRemoteIdsRegEx, noteRemoteIdsCondition));
        Matcher replaceMatcher = replacePattern.matcher(markdown);
        return replaceMatcher.replaceAll(String.format("[$1](%s$2)", RELATIVE_LINK_WORKAROUND_PREFIX));
    }
}