package kanban.ucan

object Base32 {
    private val Alphabet = "abcdefghijklmnopqrstuvwxyz234567"
    private val Lookup = Alphabet.zipWithIndex.toMap

    def encode(data: Array[Byte]): String = {
        val builder = new StringBuilder()
        var bits = 0
        var bitsLeft = 0

        for (b <- data) {
            bits = (bits << 8) | (b & 0xff)
            bitsLeft += 8
            while (bitsLeft >= 5) {
                bitsLeft -= 5
                val index = (bits >> bitsLeft) & 0x1f
                builder.append(Alphabet(index))
            }
        }

        if (bitsLeft > 0) {
            val index = (bits << (5 - bitsLeft)) & 0x1f
            builder.append(Alphabet(index))
        }

        builder.toString
    }

    def decode(str: String): Array[Byte] = {
        val buffer = new scala.collection.mutable.ArrayBuffer[Byte]()
        var bits = 0
        var bitsLeft = 0

        for (c <- str.toLowerCase) {
            Lookup.get(c) match {
                case Some(value) =>
                    bits = (bits << 5) | value
                    bitsLeft += 5
                    if (bitsLeft >= 8) {
                        bitsLeft -= 8
                        buffer += ((bits >> bitsLeft) & 0xff).toByte
                    }
                case None =>
                    throw new IllegalArgumentException(s"Invalid base32 character: $c")
            }
        }

        buffer.toArray
    }
}
