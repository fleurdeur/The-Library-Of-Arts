package org.libraryofarts.core.data.seed

import org.libraryofarts.core.model.Artist
import org.libraryofarts.core.model.ArtistConnection
import org.libraryofarts.core.model.ArtistProfile
import org.libraryofarts.core.model.ArtistRelation
import org.libraryofarts.core.model.GlossaryTerm
import org.libraryofarts.core.model.Lesson
import org.libraryofarts.core.model.LessonCategory
import org.libraryofarts.core.model.LessonSection
import org.libraryofarts.core.model.QuizKind
import org.libraryofarts.core.model.QuizQuestion
import org.libraryofarts.core.model.StoryTour
import org.libraryofarts.core.model.TourStop

object SeedLearning {
    val tours = listOf(
        StoryTour(
            id = "tour-vangogh-light",
            title = "Van Gogh's restless light",
            subtitle = "Three landscapes, one searching eye",
            artworkIds = listOf("seed:wheat-field-cypresses", "seed:bedroom", "seed:irises"),
            minutes = 12,
            stops = listOf(
                TourStop(
                    artworkId = "seed:wheat-field-cypresses",
                    title = "A field that moves",
                    narration = "Stand close enough to see the individual strokes. Van Gogh painted quickly, but the direction of every stroke is chosen. Follow the wheat from left to right; notice how the cypress interrupts the rhythm like a dark exclamation mark.",
                    seconds = 90,
                ),
                TourStop(
                    artworkId = "seed:bedroom",
                    title = "A room for rest",
                    narration = "After the turbulence of the field, the bedroom feels deliberately quiet. The tilted floor and flattened walls are not mistakes; they make the room feel intimate, as if we are seeing it from inside a memory.",
                    seconds = 80,
                ),
                TourStop(
                    artworkId = "seed:irises",
                    title = "Blue on yellow",
                    narration = "In the asylum garden, Van Gogh found a subject he could return to every day. The irises are blue-violet against yellow-green, complementary colours that make each other vibrate. One white flower pulls the eye across the canvas.",
                    seconds = 90,
                ),
            ),
        ),
        StoryTour(
            id = "tour-night",
            title = "Night, silence and the city",
            subtitle = "From Amsterdam's militia to a Chicago diner",
            artworkIds = listOf("seed:night-watch", "seed:nighthawks", "seed:thinker"),
            minutes = 10,
            stops = listOf(
                TourStop(
                    artworkId = "seed:night-watch",
                    title = "A company in motion",
                    narration = "The Night Watch is not really a night scene. Layers of varnish made it look dark for centuries. Look for the bright girl in gold; she is the company's mascot and the painting's secret sun.",
                    seconds = 90,
                ),
                TourStop(
                    artworkId = "seed:nighthawks",
                    title = "Light at the window",
                    narration = "Hopper's diner is a stage set with no audience. The glass reflects nothing; the people do not look at each other. Notice how the counter's curve leads your eye around the room and back to the street.",
                    seconds = 80,
                ),
                TourStop(
                    artworkId = "seed:thinker",
                    title = "Thought as a body",
                    narration = "Rodin's thinker is not relaxed. The toes grip the rock, the back curves, the fist meets the chin. Thought is shown as physical effort, not a floating mind.",
                    seconds = 70,
                ),
            ),
        ),
    )

    val lessons = listOf(
        Lesson(
            id = "lesson-impressionism",
            title = "Impressionism: painting the instant",
            category = LessonCategory.MOVEMENT,
            minutes = 6,
            summary = "Why a group of Paris painters traded smooth finish for visible brushwork and changing light.",
            sections = listOf(
                LessonSection(
                    "The salon problem",
                    "In the 1860s, official Paris exhibitions rewarded polished historical scenes. A younger generation wanted to paint modern life: cafés, gardens, trains and the weather. Their loose brushwork looked unfinished to critics, who borrowed the title of Monet's 'Impression, Sunrise' as an insult.",
                ),
                LessonSection(
                    "Light as a subject",
                    "Impressionists often painted the same motif at different hours. Monet's haystacks and Rouen Cathedral are experiments in how light changes colour and form. The subject matters less than the seeing.",
                ),
                LessonSection(
                    "What to look for",
                    "Look for complementary colours placed side by side, edges that dissolve, and strokes that record speed. Step back: the scene coheres. Step close: the paint becomes abstract.",
                ),
            ),
            relatedMovement = "Impressionism",
        ),
        Lesson(
            id = "lesson-pointillism",
            title = "Pointillism and the science of colour",
            category = LessonCategory.TECHNIQUE,
            minutes = 5,
            summary = "Seurat's dots, optical mixing and the dream of a rational art.",
            sections = listOf(
                LessonSection("Dots as pixels", "Pointillism places small touches of pure colour next to one another. From a distance the eye blends them, a phenomenon called optical mixing."),
                LessonSection("A calculated surface", "Seurat made dozens of preparatory drawings and studies. The finished Grande Jatte is not spontaneous; it is engineered quiet."),
                LessonSection("Try it", "Pick two colours that vibrate together. Make a small grid of alternating dots and view it from one metre away. Notice when the colours begin to merge."),
            ),
        ),
        Lesson(
            id = "lesson-baroque",
            title = "Baroque light: staging the sacred and the civic",
            category = LessonCategory.MOVEMENT,
            minutes = 7,
            summary = "How Caravaggio, Rembrandt and Vermeer used light to organise attention.",
            sections = listOf(
                LessonSection("Chiaroscuro", "Baroque painters push light and dark together to create volume and drama. In Rembrandt, darkness is not empty; it is a place where faces emerge."),
                LessonSection("Daylight interiors", "Vermeer's light comes from a window at the left, calm and even. His interiors feel private because the light has a direction we can trace."),
                LessonSection("Look for the source", "Before reading the subject, find the light source. Ask what it reveals and what it hides."),
            ),
        ),
        Lesson(
            id = "lesson-reading-symbols",
            title = "How to read a painting's symbols",
            category = LessonCategory.READING,
            minutes = 8,
            summary = "A practical method for spotting meaning without over-reading.",
            sections = listOf(
                LessonSection("Start with the obvious", "List what is actually there: objects, gestures, setting, colours. Interpretation begins with inventory."),
                LessonSection("Ask about conventions", "A dog can mean fidelity, a skull mortality, a mirror vanity. Meaning depends on the period and the patron, not on a universal dictionary."),
                LessonSection("Test your reading", "A strong symbolic reading explains several details at once. If it requires ignoring half the picture, it is probably too narrow."),
            ),
        ),
        Lesson(
            id = "lesson-conservation",
            title = "What restoration reveals",
            category = LessonCategory.MATERIALS,
            minutes = 6,
            summary = "Varnish, X-rays and the ethical choices behind cleaning a masterpiece.",
            sections = listOf(
                LessonSection("Yellow varnish", "Old varnish darkens and yellows. Cleaning can reveal cooler colours, but it also removes evidence of the painting's history."),
                LessonSection("Seeing underneath", "Infrared can reveal underdrawing; X-rays show lead white and hidden compositions. These layers are part of the object, not a secret original."),
                LessonSection("The Rembrandt question", "The Night Watch was long read as a night scene because of darkened varnish. Restoration changed how we understand its light."),
            ),
        ),
        Lesson(
            id = "lesson-perspective",
            title = "Perspective, tilting floors and modern space",
            category = LessonCategory.TECHNIQUE,
            minutes = 5,
            summary = "Why some paintings refuse correct perspective — deliberately.",
            sections = listOf(
                LessonSection("One-point perspective", "Renaissance artists used a vanishing point to make space measurable. It is a convention, not the way eyes actually see."),
                LessonSection("Flattening", "Van Gogh tilts the bedroom floor and Cassatt raises the viewpoint. These choices compress space and intensify feeling."),
                LessonSection("Look for the horizon", "Find where the horizon sits. A high horizon makes the ground dominate; a low one makes the sky loom."),
            ),
        ),
        Lesson(
            id = "lesson-gold",
            title = "Gold leaf and the Vienna Secession",
            category = LessonCategory.MATERIALS,
            minutes = 4,
            summary = "Klimt's gold is material, symbol and surface at once.",
            sections = listOf(
                LessonSection("A craft revival", "Klimt learned gold leaf from his father, a goldsmith. In The Kiss, gold flattens the couple into an icon while making the surface physically precious."),
                LessonSection("Pattern as emotion", "The rectangles on the man's robe and circles on the woman's dress are not decoration. They distinguish two bodies while binding them together."),
            ),
        ),
        Lesson(
            id = "lesson-slow-looking",
            title = "Slow looking: a five-minute practice",
            category = LessonCategory.READING,
            minutes = 5,
            summary = "A timed exercise that turns viewing into attention.",
            sections = listOf(
                LessonSection("Minute one", "Name the first thing you notice. Do not judge it."),
                LessonSection("Minute two", "Follow one colour through the picture. Where does it repeat?"),
                LessonSection("Minute three", "Find the smallest detail you can see. Describe it without interpretation."),
                LessonSection("Minute four", "Notice where your eyes rest. That is often the emotional centre."),
                LessonSection("Minute five", "Write one sentence you would tell a friend. Keep it."),
            ),
        ),
    )

    val glossary = listOf(
        GlossaryTerm("Chiaroscuro", "The strong contrast of light and dark used to model form and create drama.", "Technique", listOf("Baroque", "Rembrandt")),
        GlossaryTerm("Sfumato", "A smoky, soft blending of tones and edges, perfected by Leonardo da Vinci.", "Technique", listOf("Renaissance", "Leonardo")),
        GlossaryTerm("Impasto", "Paint applied thickly so that brush or palette-knife marks remain visible.", "Technique", listOf("Van Gogh", "Impressionism")),
        GlossaryTerm("Pointillism", "A technique of small, distinct dots of colour that blend optically.", "Technique", listOf("Seurat")),
        GlossaryTerm("Tronie", "A Dutch study of a face or expression rather than a specific sitter.", "Genre", listOf("Vermeer", "Rembrandt")),
        GlossaryTerm("Provenance", "The documented history of ownership of an artwork.", "Museum practice"),
        GlossaryTerm("Accession number", "The unique identifier a museum assigns to an object.", "Museum practice"),
        GlossaryTerm("Ukiyo-e", "Japanese 'pictures of the floating world', often woodblock prints.", "Movement", listOf("Hokusai")),
        GlossaryTerm("IIIF", "International Image Interoperability Framework, a standard for deep-zoom images.", "Technology"),
        GlossaryTerm("Complementary colours", "Colours opposite each other on the colour wheel, which intensify when placed side by side.", "Colour", listOf("Van Gogh")),
        GlossaryTerm("Chiaroscuro", "The use of strong contrasts between light and dark.", "Technique"),
        GlossaryTerm("Vanishing point", "The point where parallel lines appear to converge in linear perspective.", "Technique"),
        GlossaryTerm("Sitter", "The person depicted in a portrait.", "Portraiture"),
        GlossaryTerm("Genre painting", "Scenes of everyday life rather than history or mythology.", "Genre"),
        GlossaryTerm("Tempera", "Pigment bound with egg yolk, common before oil paint.", "Materials"),
        GlossaryTerm("Gold leaf", "Thin sheets of gold applied to a surface as decoration.", "Materials"),
        GlossaryTerm("Conservation", "Stabilising and caring for an artwork, distinct from restoration.", "Museum practice"),
        GlossaryTerm("Restoration", "Intervention intended to return an artwork to a former appearance.", "Museum practice"),
        GlossaryTerm("Underdrawing", "A preliminary sketch beneath the paint, often visible with infrared.", "Technique"),
        GlossaryTerm("Pentimento", "A visible change of mind in a painting, where an earlier image shows through.", "Technique"),
    ).distinctBy { it.term }

    val artists = listOf(
        ArtistProfile(
            artist = Artist("van-gogh", "Vincent van Gogh", 1853, 1890, "Dutch"),
            summary = "Van Gogh painted for only about a decade, yet produced more than 2,000 works. His letters describe colour as emotional weather, and his brushwork as a way to make feeling visible.",
            movements = listOf("Post-Impressionism"),
            keyWorks = listOf("seed:wheat-field-cypresses", "seed:bedroom", "seed:irises"),
            connections = listOf(
                ArtistConnection("Vincent van Gogh", "Paul Gauguin", ArtistRelation.RIVAL, "Brief, explosive cohabitation in Arles in 1888."),
                ArtistConnection("Vincent van Gogh", "Claude Monet", ArtistRelation.INFLUENCE, "Shared interest in colour and open-air light, though Van Gogh's line is more urgent."),
            ),
        ),
        ArtistProfile(
            artist = Artist("rembrandt", "Rembrandt van Rijn", 1606, 1669, "Dutch"),
            summary = "Rembrandt moved from smooth, fashionable history painting to a deeply psychological use of light. He painted merchants, mothers, self-portraits and biblical scenes with equal seriousness.",
            movements = listOf("Baroque", "Dutch Golden Age"),
            keyWorks = listOf("seed:night-watch", "seed:aristotle"),
        ),
        ArtistProfile(
            artist = Artist("vermeer", "Johannes Vermeer", 1632, 1675, "Dutch"),
            summary = "Only about 35 paintings are attributed to Vermeer. He worked slowly, building cool, luminous interiors where a window, a map or a single nail can carry the whole emotional weight.",
            movements = listOf("Dutch Golden Age"),
            keyWorks = listOf("seed:milkmaid", "seed:pearl-earring"),
        ),
        ArtistProfile(
            artist = Artist("leonardo", "Leonardo da Vinci", 1452, 1519, "Italian"),
            summary = "Painter, anatomist, engineer and notebook-keeper. Leonardo's surviving paintings are few, but his experiments with sfumato and observation changed what a portrait could feel like.",
            movements = listOf("Italian Renaissance"),
            keyWorks = listOf("seed:ginevra", "seed:mona-lisa"),
        ),
        ArtistProfile(
            artist = Artist("monet", "Claude Monet", 1840, 1926, "French"),
            summary = "Monet painted light as it changed, often returning to the same subject dozens of times. At Giverny he built a water garden that became his final, immersive studio.",
            movements = listOf("Impressionism"),
            keyWorks = listOf("seed:water-lilies"),
        ),
        ArtistProfile(
            artist = Artist("klimt", "Gustav Klimt", 1862, 1918, "Austrian"),
            summary = "Klimt combined figure, ornament and gold into a style that made Vienna modern and luxurious at once. His portraits are as much surfaces as they are people.",
            movements = listOf("Vienna Secession"),
            keyWorks = listOf("seed:klimt-kiss"),
        ),
    )

    val quiz = listOf(
        QuizQuestion("q1", "Who painted The Harvesters?", listOf("Pieter Bruegel the Elder", "Rembrandt", "Vermeer", "Van Gogh"), 0, "Bruegel painted the seasons for a Antwerp merchant in 1565.", "seed:harvesters", QuizKind.GUESS_THE_ARTIST),
        QuizQuestion("q2", "Which technique uses tiny dots of colour?", listOf("Impasto", "Pointillism", "Sfumato", "Chiaroscuro"), 1, "Seurat's Grande Jatte is the landmark of pointillism.", "seed:sunday-grande-jatte", QuizKind.GENERAL),
        QuizQuestion("q3", "What is a tronie?", listOf("A Dutch group portrait", "A study of a face or type", "A gold-ground altarpiece", "A museum catalogue"), 1, "Girl with a Pearl Earring is a tronie, not a portrait of a named sitter.", "seed:pearl-earring", QuizKind.GENERAL),
        QuizQuestion("q4", "Which museum holds The Night Watch?", listOf("The Met", "Rijksmuseum", "Getty", "AIC"), 1, "The Rijksmuseum in Amsterdam houses Rembrandt's militia portrait.", "seed:night-watch", QuizKind.GENERAL),
        QuizQuestion("q5", "Vermeer's Milkmaid is admired for its…", listOf("Gold leaf", "Cool, precise light", "Violent movement", "Collage"), 1, "The light from the window models the milk, the bread and the wall with extraordinary care.", "seed:milkmaid", QuizKind.GENERAL),
        QuizQuestion("q6", "Who painted The Bedroom at Arles?", listOf("Monet", "Van Gogh", "Renoir", "Hopper"), 1, "Van Gogh painted three versions; this one is in Chicago.", "seed:bedroom", QuizKind.GUESS_THE_ARTIST),
        QuizQuestion("q7", "American Gothic was painted by…", listOf("Grant Wood", "Edward Hopper", "Mary Cassatt", "John Singer Sargent"), 0, "Wood's regionalist image was painted in Iowa in 1930.", "seed:american-gothic", QuizKind.GUESS_THE_ARTIST),
        QuizQuestion("q8", "What does sfumato mean?", listOf("A smoky blending of tones", "A thick paint application", "A gilded background", "A type of frame"), 0, "Leonardo's soft edges are the classic example.", "seed:mona-lisa", QuizKind.GENERAL),
        QuizQuestion("q9", "Which work is a woodblock print?", listOf("The Great Wave", "Irises", "The Thinker", "The Kiss"), 0, "Hokusai's Great Wave is a nishiki-e woodblock print.", "seed:great-wave", QuizKind.GENERAL),
        QuizQuestion("q10", "The Thinker was originally conceived for…", listOf("The Gates of Hell", "The Night Watch", "A Paris fountain", "A tomb"), 0, "Rodin designed the figure for his monumental Gates of Hell.", "seed:thinker", QuizKind.GENERAL),
        QuizQuestion("q11", "Which pair of colours is complementary?", listOf("Blue and orange", "Blue and green", "Red and orange", "Yellow and green"), 0, "Blue and orange sit opposite on the colour wheel.", "seed:irises", QuizKind.GENERAL),
        QuizQuestion("q12", "The 'pearl' in Girl with a Pearl Earring may actually be…", listOf("A mirror", "Polished tin or glass", "A diamond", "A drop of water"), 1, "Two highlights suggest an imagined reflection rather than a simple pearl.", "seed:pearl-earring", QuizKind.SPOT_THE_FAKE),
    )

    val onThisDay = listOf(
        OnThisDayEvent(3, 30, "Vincent van Gogh was born in Zundert in 1853.", "seed:wheat-field-cypresses"),
        OnThisDayEvent(7, 15, "Rembrandt van Rijn was born in Leiden in 1606.", "seed:night-watch"),
        OnThisDayEvent(11, 14, "Claude Monet was born in Paris in 1840.", "seed:water-lilies"),
        OnThisDayEvent(12, 2, "Georges Seurat was born in Paris in 1859.", "seed:sunday-grande-jatte"),
        OnThisDayEvent(7, 14, "Gustav Klimt was born near Vienna in 1862.", "seed:klimt-kiss"),
        OnThisDayEvent(11, 12, "Auguste Rodin was born in Paris in 1840.", "seed:thinker"),
        OnThisDayEvent(5, 22, "Mary Cassatt was born in Allegheny City in 1844.", "seed:childs-bath"),
        OnThisDayEvent(2, 24, "Winslow Homer was born in Boston in 1836.", "seed:nighthawks"),
        OnThisDayEvent(1, 12, "John Singer Sargent was born in Florence in 1856.", "seed:madame-x"),
        OnThisDayEvent(7, 6, "Frida Kahlo was born in Coyoacán in 1907.", "seed:two-sisters"),
        OnThisDayEvent(10, 31, "Johannes Vermeer was baptised in Delft in 1632.", "seed:milkmaid"),
        OnThisDayEvent(3, 6, "Michelangelo Buonarroti was born in Caprese in 1475.", "seed:birth-of-venus"),
    )
}

data class OnThisDayEvent(
    val month: Int,
    val day: Int,
    val text: String,
    val artworkId: String,
)
