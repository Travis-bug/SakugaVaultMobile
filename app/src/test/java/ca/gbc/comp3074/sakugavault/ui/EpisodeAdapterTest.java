package ca.gbc.comp3074.sakugavault.ui;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import ca.gbc.comp3074.sakugavault.api.dto.WatchEpisodeDto;

/** Unit tests: episode labels in the episode list. */
public class EpisodeAdapterTest {

    private static WatchEpisodeDto episode(int number, String label) {
        WatchEpisodeDto dto = new WatchEpisodeDto();
        dto.episodeNumber = number;
        dto.label = label;
        return dto;
    }

    @Test
    public void missingLabelShowsJustTheNumber() {
        assertEquals("Episode 1", EpisodeAdapter.label(episode(1, null)));
        assertEquals("Episode 1", EpisodeAdapter.label(episode(1, "")));
        assertEquals("Episode 1", EpisodeAdapter.label(episode(1, "Episode 1")));
    }

    @Test
    public void repeatedEpisodeNumberIsDroppedOnce() {
        assertEquals("Episode 1", EpisodeAdapter.label(episode(1, "Episode 1 · Episode 1")));
    }

    @Test
    public void realTitleIsKept() {
        assertEquals("Episode 3 · The Journey's End", EpisodeAdapter.label(episode(3, "Episode 3 · The Journey's End")));
        assertEquals("Episode 3 · The Journey's End", EpisodeAdapter.label(episode(3, "The Journey's End")));
    }

    @Test
    public void similarNumbersAreNotStripped() {
        // "Episode 12" must not be treated as "Episode 1" + "2".
        assertEquals("Episode 1 · Episode 12", EpisodeAdapter.label(episode(1, "Episode 12")));
    }

    @Test
    public void prefixMatchingIgnoresCaseAndSeparators() {
        assertEquals("Episode 7 · Reunion", EpisodeAdapter.label(episode(7, "EPISODE 7: Reunion")));
        assertEquals("Episode 7 · Reunion", EpisodeAdapter.label(episode(7, "episode7 - Reunion")));
    }
}
